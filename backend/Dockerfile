FROM node:22-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install --omit=dev
COPY src ./src
RUN mkdir -p uploads
ENV PORT=10000
EXPOSE 10000
CMD ["npm","start"]
