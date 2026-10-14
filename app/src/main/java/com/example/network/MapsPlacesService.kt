package com.example.network

import android.location.Location
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class NearbyHealthcarePlace(
  val id: String,
  val name: String,
  val address: String,
  val latitude: Double,
  val longitude: Double,
  val phoneNumber: String,
  val rating: Double?,
  val userRatingCount: Int?,
  val categoryLabel: String,
  val distanceMeters: Float,
  val googleMapsUri: String,
  val directionsUri: String,
)

data class RouteDirectionsResult(
  val distanceMeters: Int,
  val durationText: String,
  val steps: List<String>,
)

sealed class MapsApiResult<out T> {
  data class Success<T>(val data: T) : MapsApiResult<T>()
  data object QuotaExceeded : MapsApiResult<Nothing>()
  data class Error(val message: String) : MapsApiResult<Nothing>()
}

enum class FacilityFilterType(val includedTypes: List<String>) {
  HOSPITALS(listOf("hospital")),
  CLINICS(listOf("doctor", "medical_lab", "dental_clinic", "physiotherapist")),
  PHARMACIES(listOf("pharmacy")),
}

object MapsPlacesService {
  private const val ATTRIBUTION_ID = "gmp_mcp_codeassist_v1_aistudio"
  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  private val httpClient =
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(15, TimeUnit.SECONDS)
      .build()

  suspend fun searchNearbyFacilities(
    userLat: Double,
    userLng: Double,
    filterType: FacilityFilterType,
    radiusMeters: Double = 6000.0,
    languageCode: String = "en",
  ): MapsApiResult<List<NearbyHealthcarePlace>> =
    withContext(Dispatchers.IO) {
      val apiKey = BuildConfig.MAPS_API_KEY
      if (apiKey.isBlank()) {
        return@withContext MapsApiResult.Error(
          "Google Maps API key is not configured in BuildConfig."
        )
      }

      try {
        // Reference: https://developers.google.com/maps/documentation/places/web-service/nearby-search?utm_campaign=gmp_mcp_codeassist_v1_aistudio
        val requestJson =
          JSONObject().apply {
            put("includedTypes", JSONArray(filterType.includedTypes))
            put("maxResultCount", 15)
            put("languageCode", languageCode)
            put(
              "locationRestriction",
              JSONObject().apply {
                put(
                  "circle",
                  JSONObject().apply {
                    put(
                      "center",
                      JSONObject().apply {
                        put("latitude", userLat)
                        put("longitude", userLng)
                      },
                    )
                    put("radius", radiusMeters)
                  },
                )
              },
            )
          }

        val fieldMask =
          "places.id,places.displayName,places.formattedAddress,places.location," +
            "places.nationalPhoneNumber,places.internationalPhoneNumber,places.rating," +
            "places.userRatingCount,places.primaryType,places.primaryTypeDisplayName," +
            "places.googleMapsUri,places.googleMapsLinks"

        val request =
          Request.Builder()
            .url("https://places.googleapis.com/v1/places:searchNearby")
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Goog-Api-Key", apiKey)
            .addHeader("X-Goog-FieldMask", fieldMask)
            .addHeader("X-Goog-Maps-Solution-ID", ATTRIBUTION_ID)
            .build()

        httpClient.newCall(request).execute().use { response ->
          val bodyStr = response.body?.string().orEmpty()
          if (response.code == 429 || bodyStr.contains("RESOURCE_EXHAUSTED") || bodyStr.contains("OVER_QUERY_LIMIT")) {
            return@withContext MapsApiResult.QuotaExceeded
          }
          if (!response.isSuccessful) {
            return@withContext MapsApiResult.Error(
              "Places API error (${response.code}): Unable to load nearby healthcare facilities."
            )
          }

          val root = JSONObject(bodyStr.ifBlank { "{}" })
          val placesArray = root.optJSONArray("places") ?: JSONArray()
          val results = mutableListOf<NearbyHealthcarePlace>()

          for (i in 0 until placesArray.length()) {
            val placeObj = placesArray.optJSONObject(i) ?: continue
            val id = placeObj.optString("id", "")
            val displayName = placeObj.optJSONObject("displayName")?.optString("text", "") ?: ""
            if (displayName.isBlank()) continue

            val formattedAddress = placeObj.optString("formattedAddress", "")
            val locObj = placeObj.optJSONObject("location")
            val lat = locObj?.optDouble("latitude", userLat) ?: userLat
            val lng = locObj?.optDouble("longitude", userLng) ?: userLng

            val distResults = FloatArray(1)
            Location.distanceBetween(userLat, userLng, lat, lng, distResults)
            val distMeters = distResults[0]

            val phone =
              placeObj
                .optString("nationalPhoneNumber", "")
                .ifBlank { placeObj.optString("internationalPhoneNumber", "") }

            val rating = if (placeObj.has("rating")) placeObj.optDouble("rating") else null
            val ratingCount =
              if (placeObj.has("userRatingCount")) placeObj.optInt("userRatingCount") else null

            val primaryTypeDisplay =
              placeObj.optJSONObject("primaryTypeDisplayName")?.optString("text", "")
                ?: placeObj.optString("primaryType", "Healthcare Facility")

            val googleMapsUri = placeObj.optString("googleMapsUri", "")
            val directionsUri =
              placeObj.optJSONObject("googleMapsLinks")?.optString("directionsUri", "") ?: ""

            results.add(
              NearbyHealthcarePlace(
                id = id,
                name = displayName,
                address = formattedAddress,
                latitude = lat,
                longitude = lng,
                phoneNumber = phone,
                rating = rating,
                userRatingCount = ratingCount,
                categoryLabel = primaryTypeDisplay,
                distanceMeters = distMeters,
                googleMapsUri = googleMapsUri,
                directionsUri = directionsUri,
              )
            )
          }

          results.sortBy { it.distanceMeters }
          return@withContext MapsApiResult.Success(results)
        }
      } catch (e: Exception) {
        return@withContext MapsApiResult.Error(
          "Network error while querying nearby facilities: ${e.localizedMessage ?: "Unknown error"}"
        )
      }
    }

  suspend fun computeRouteDirections(
    originLat: Double,
    originLng: Double,
    destLat: Double,
    destLng: Double,
  ): MapsApiResult<RouteDirectionsResult> =
    withContext(Dispatchers.IO) {
      val apiKey = BuildConfig.MAPS_API_KEY
      if (apiKey.isBlank()) {
        return@withContext MapsApiResult.Error("Google Maps API key is missing.")
      }

      try {
        // Reference: https://developers.google.com/maps/documentation/routes/compute_route_directions?utm_campaign=gmp_mcp_codeassist_v1_aistudio
        val requestJson =
          JSONObject().apply {
            put(
              "origin",
              JSONObject().apply {
                put(
                  "location",
                  JSONObject().apply {
                    put(
                      "latLng",
                      JSONObject().apply {
                        put("latitude", originLat)
                        put("longitude", originLng)
                      },
                    )
                  },
                )
              },
            )
            put(
              "destination",
              JSONObject().apply {
                put(
                  "location",
                  JSONObject().apply {
                    put(
                      "latLng",
                      JSONObject().apply {
                        put("latitude", destLat)
                        put("longitude", destLng)
                      },
                    )
                  },
                )
              },
            )
            put("travelMode", "DRIVE")
            put("routingPreference", "TRAFFIC_AWARE")
            put("internalUsageAttributionIds", JSONArray(listOf(ATTRIBUTION_ID)))
          }

        val fieldMask =
          "routes.duration,routes.distanceMeters,routes.legs.steps.navigationInstruction"

        val request =
          Request.Builder()
            .url("https://routes.googleapis.com/directions/v2:computeRoutes")
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Goog-Api-Key", apiKey)
            .addHeader("X-Goog-FieldMask", fieldMask)
            .addHeader("X-Goog-Maps-Solution-ID", ATTRIBUTION_ID)
            .build()

        httpClient.newCall(request).execute().use { response ->
          val bodyStr = response.body?.string().orEmpty()
          if (response.code == 429 || bodyStr.contains("RESOURCE_EXHAUSTED") || bodyStr.contains("OVER_QUERY_LIMIT")) {
            return@withContext MapsApiResult.QuotaExceeded
          }
          if (!response.isSuccessful) {
            return@withContext MapsApiResult.Error("Routes API error (${response.code})")
          }

          val root = JSONObject(bodyStr.ifBlank { "{}" })
          val routeObj = root.optJSONArray("routes")?.optJSONObject(0)
            ?: return@withContext MapsApiResult.Error("No route found between locations.")

          val distanceMeters = routeObj.optInt("distanceMeters", 0)
          val rawDurationSec =
            routeObj.optString("duration", "0s").removeSuffix("s").toIntOrNull() ?: 0
          val durationMin = (rawDurationSec / 60).coerceAtLeast(1)

          val stepsList = mutableListOf<String>()
          val legs = routeObj.optJSONArray("legs")
          if (legs != null && legs.length() > 0) {
            val steps = legs.optJSONObject(0)?.optJSONArray("steps")
            if (steps != null) {
              for (i in 0 until steps.length()) {
                val instr =
                  steps
                    .optJSONObject(i)
                    ?.optJSONObject("navigationInstruction")
                    ?.optString("instructions", "")
                    .orEmpty()
                if (instr.isNotBlank()) {
                  stepsList.add(instr)
                }
              }
            }
          }

          return@withContext MapsApiResult.Success(
            RouteDirectionsResult(
              distanceMeters = distanceMeters,
              durationText = "$durationMin min",
              steps = stepsList,
            )
          )
        }
      } catch (e: Exception) {
        return@withContext MapsApiResult.Error(
          "Unable to calculate route: ${e.localizedMessage ?: "Network error"}"
        )
      }
    }

  fun buildInteractiveMapHtml(
    userLat: Double,
    userLng: Double,
    places: List<NearbyHealthcarePlace>,
  ): String {
    val apiKey = BuildConfig.MAPS_API_KEY
    val markersJson = JSONArray()
    places.forEach { p ->
      markersJson.put(
        JSONObject().apply {
          put("name", p.name)
          put("lat", p.latitude)
          put("lng", p.longitude)
          put("address", p.address)
        }
      )
    }

    return """
      <!DOCTYPE html>
      <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <style>
          html, body, #map { margin: 0; padding: 0; width: 100%; height: 100%; font-family: sans-serif; }
        </style>
        <script>
          window.gm_authFailure = function() {
            if (window.AndroidBridge && window.AndroidBridge.onQuotaExceeded) {
              window.AndroidBridge.onQuotaExceeded();
            }
          };
          const origError = console.error;
          console.error = function() {
            origError.apply(console, arguments);
            const msg = Array.from(arguments).map(String).join(' ');
            if (msg.includes('OverQuotaMapError') || msg.includes('QuotaExceededError')) {
              if (window.AndroidBridge && window.AndroidBridge.onQuotaExceeded) {
                window.AndroidBridge.onQuotaExceeded();
              }
            }
          };
        </script>
      </head>
      <body>
        <div id="map"></div>
        <script>
          (g=>{var h,a,k,p="The Google Maps JavaScript API",c="google",l="importLibrary",q="__ib__",m=document,b=window;b=b[c]||(b[c]={});var d=b.maps||(b.maps={}),r=new Set,e=new URLSearchParams,u=()=>h||(h=new Promise(async(f,n)=>{await (a=m.createElement("script"));e.set("libraries",[...r]+"");for(k in g)e.set(k.replace(/[A-Z]/g,t=>"_"+t[0].toLowerCase()),g[k]);e.set("callback",c+".maps."+q);a.src=`https://maps.${'$'}{c}apis.com/maps/api/js?`+e;d[q]=f;a.onerror=()=>h=n(Error(p+" could not load."));a.nonce=m.querySelector("script[nonce]")?.nonce||"";m.head.append(a)}));d[l]?console.warn(p+" only loads once. Ignoring:",g):d[l]=(f,...n)=>r.add(f)&&u().then(()=>d[l](f,...n))})({
            key: "$apiKey",
            v: "weekly"
          });

          const userPos = { lat: $userLat, lng: $userLng };
          const facilities = ${markersJson.toString()};

          async function initMap() {
            const { Map, InfoWindow } = await google.maps.importLibrary("maps");
            const { AdvancedMarkerElement, PinElement } = await google.maps.importLibrary("marker");

            const map = new Map(document.getElementById("map"), {
              center: userPos,
              zoom: 14,
              mapId: "DEMO_MAP_ID",
              internalUsageAttributionIds: ["$ATTRIBUTION_ID"],
              disableDefaultUI: false
            });

            const infoWindow = new InfoWindow();

            const userPin = new PinElement({
              background: "#0284C7",
              borderColor: "#FFFFFF",
              glyphColor: "#FFFFFF",
              glyphText: "YOU"
            });
            const userMarker = new AdvancedMarkerElement({
              map: map,
              position: userPos,
              title: "Your Current Location"
            });
            userMarker.append(userPin);

            facilities.forEach((item, idx) => {
              const pin = new PinElement({
                background: "#0F766E",
                borderColor: "#FFFFFF",
                glyphColor: "#FFFFFF",
                glyphText: String(idx + 1)
              });
              const marker = new AdvancedMarkerElement({
                map: map,
                position: { lat: item.lat, lng: item.lng },
                title: item.name
              });
              marker.append(pin);
              marker.addListener("click", () => {
                infoWindow.setContent("<strong>" + item.name + "</strong><br/><small>" + item.address + "</small>");
                infoWindow.open({ anchor: marker, map });
              });
            });
          }
          initMap();
        </script>
      </body>
      </html>
    """.trimIndent()
  }

  fun formatDistance(meters: Float): String {
    return if (meters < 1000f) {
      "${meters.toInt()} m"
    } else {
      String.format("%.2f km", meters / 1000f)
    }
  }
}
