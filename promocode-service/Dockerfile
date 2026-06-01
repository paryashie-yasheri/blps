FROM golang:1.26.2-alpine AS builder

WORKDIR /app

COPY go.mod go.sum ./
RUN go mod download

COPY . .
RUN go build -o /out/promocode-service .

FROM alpine:3.22

WORKDIR /app

COPY --from=builder /out/promocode-service /app/promocode-service

ENTRYPOINT ["/app/promocode-service"]
