FROM node:22-slim

WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm install --omit=dev

# Copy application assets and server files
COPY . .

# Set default production port for Cloud Run
ENV PORT=8080
ENV NODE_ENV=production
EXPOSE 8080

CMD ["node", "server.cjs"]
