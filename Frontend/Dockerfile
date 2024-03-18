FROM node
COPY ./ ./
RUN npm install
ENTRYPOINT [ "npm", "run", "preview", "--","--host" ]