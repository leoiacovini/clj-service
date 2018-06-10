# clj-service

A Clojure library designed to help building microservices for the LabSoft 2 course. 

## Usage

### Creating keys for token signing:

```
openssl genrsa -out privkey.pem 2048
openssl rsa -pubout -in privkey.pem -out pubkey.pem
```

FIXME
