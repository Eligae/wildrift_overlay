FROM node:20-alpine

WORKDIR /app

COPY package*.json tsconfig.json ./
RUN npm ci

COPY src ./src
COPY data ./data

RUN npm run build

ENV NODE_ENV=production
EXPOSE 3000

CMD ["npm", "start"]
