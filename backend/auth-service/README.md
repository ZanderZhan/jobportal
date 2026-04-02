# Auth Service

## JWT Key Setup for Local Development

### Generate Keys

```bash
mkdir -p ~/.jwt/keys
openssl genrsa -out ~/.jwt/keys/jwt-private.pem 2048
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in ~/.jwt/keys/jwt-private.pem -out ~/.jwt/keys/jwt-private-key.pem
openssl rsa -in ~/.jwt/keys/jwt-private.pem -pubout -outform PEM -out ~/.jwt/keys/jwt-public-key.pem
```

### Run

```bash
export JWT_PRIVATE_KEY_FILE=~/.jwt/keys/jwt-private-key.pem
export JWT_PUBLIC_KEY_FILE=~/.jwt/keys/jwt-public-key.pem
./gradlew bootRun
```
