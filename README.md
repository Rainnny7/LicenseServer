# LicenseServer

A simple open-source licensing server for your products.

## Discord Preview

![License Global Log](https://cdn.rainnny.club/SagsCD0I.png)
![License Owner Log](https://cdn.rainnny.club/JZdFxTCy.png)

## API Reference

### Check License

```http
POST /check
```

#### Body

| Key       | Type     | Description                                    |
|:----------|:---------|:-----------------------------------------------|
| `key`     | `string` | **Required**. Your license key                 |
| `product` | `string` | **Required**. The product the license is for   |
| `hwid`    | `string` | **Required**. The hardware id of the requester |

#### Response

##### Error

```json
{
  "error": "Error message"
}
```

##### Success

```json
{
  "description": "Testing",
  "ownerSnowflake": 504147739131641857,
  "ownerName": "Braydon#2712",
  "duration": -1
}
```

## Deployment

### Docker

```bash
docker run -d -p 7500:7500 -v "$(pwd)/data/application.yml:/usr/local/app/application.yml" git.rainnny.club/rainnny/licenseserver:latest  
```

### Docker Compose

```yml
version: '3'
services:
  app:
    image: git.rainnny.club/rainnny/licenseserver:latest
    volumes:
      - ./data/application.yml:/usr/local/app/application.yml
    ports:
      - "7500:7500"
```