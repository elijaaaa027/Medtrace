#!/bin/bash
set -euo pipefail

# ==============================================================================
# MedTrace - Production Deployment Script for Google Cloud Run
# Fixes "Failed to import image gs://run-sources-.../app.zip" by building
# the container image cleanly or passing the verified app.tar.gz archive.
# ==============================================================================

SERVICE_NAME="${SERVICE_NAME:-medtrace-0}"
REGION="${REGION:-asia-east1}"
PORT="${PORT:-8080}"
ARCHIVE_NAME="app.tar.gz"

echo "======================================================================"
echo " Preparing MedTrace for Google Cloud Run Deployment"
echo " Service:  ${SERVICE_NAME}"
echo " Region:   ${REGION}"
echo " Port:     ${PORT}"
echo "======================================================================"

# 1. Package the source archive using package-tarball.cjs
node package-tarball.cjs

if [ ! -f "${ARCHIVE_NAME}" ]; then
  echo "Error: ${ARCHIVE_NAME} was not generated!"
  exit 1
fi

ARCHIVE_SIZE=$(ls -lh "${ARCHIVE_NAME}" | awk '{print $5}')
echo "Verified source tarball ready: ${ARCHIVE_NAME} (${ARCHIVE_SIZE})"

# 2. Check if gcloud is installed
if command -v gcloud >/dev/null 2>&1; then
  PROJECT_ID=$(gcloud config get-value project 2>/dev/null || echo "")
  
  if [ -n "${PROJECT_ID}" ] && [ "${PROJECT_ID}" != "(unset)" ]; then
    echo "Using Google Cloud Project: ${PROJECT_ID}"
    IMAGE_TAG="gcr.io/${PROJECT_ID}/${SERVICE_NAME}:latest"
    
    echo "======================================================================"
    echo "Method 1: Building container image with Cloud Build (Prevents ZIP errors)"
    echo "======================================================================"
    gcloud builds submit --tag "${IMAGE_TAG}" .
    
    echo "Deploying built container image to Cloud Run..."
    gcloud run deploy "${SERVICE_NAME}" \
      --image="${IMAGE_TAG}" \
      --region="${REGION}" \
      --platform=managed \
      --allow-unauthenticated \
      --port="${PORT}" \
      --set-env-vars=NODE_ENV=production
  else
    echo "Deploying via source archive: ${ARCHIVE_NAME}..."
    gcloud run deploy "${SERVICE_NAME}" \
      --source="./${ARCHIVE_NAME}" \
      --region="${REGION}" \
      --platform=managed \
      --allow-unauthenticated \
      --port="${PORT}" \
      --set-env-vars=NODE_ENV=production
  fi
else
  echo ""
  echo "======================================================================"
  echo " Packaging Complete! [${ARCHIVE_NAME}] is ready."
  echo ""
  echo " To fix the 'Failed to import image app.zip' error, run either command"
  echo " in Google Cloud Shell or your local terminal:"
  echo ""
  echo " ── Option A (Recommended - Builds image first, 100% bypasses zip errors) ──"
  echo "   gcloud builds submit --tag gcr.io/\$(gcloud config get-value project)/${SERVICE_NAME}:latest"
  echo "   gcloud run deploy ${SERVICE_NAME} \\"
  echo "     --image=gcr.io/\$(gcloud config get-value project)/${SERVICE_NAME}:latest \\"
  echo "     --region=${REGION} \\"
  echo "     --allow-unauthenticated \\"
  echo "     --port=${PORT}"
  echo ""
  echo " ── Option B (Deploys explicitly from the verified tar.gz archive) ──"
  echo "   gcloud run deploy ${SERVICE_NAME} \\"
  echo "     --source=./${ARCHIVE_NAME} \\"
  echo "     --region=${REGION} \\"
  echo "     --allow-unauthenticated \\"
  echo "     --port=${PORT}"
  echo "======================================================================"
fi
