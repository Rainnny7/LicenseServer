[![Discord](https://discord.com/api/guilds/827863713855176755/widget.png)](https://discord.gg/p9gzFE2bc6)
![Wakatime Hours](https://wakatime.rainnny.club/api/badge/Rainnny/interval:any/project:LicenseServer)
[![Download](https://img.shields.io/badge/Download-Releases-darkgreen.svg)](https://git.rainnny.club/Rainnny/LicenseServer/releases)

# LicenseServer
A simple open-source licensing server for your products.

## Discord Preview

![License Global Log](https://cdn.rainnny.club/SagsCD0I.png)
![License Owner Log](https://cdn.rainnny.club/JZdFxTCy.png)
![License Owner Lookup](https://cdn.rainnny.club/EU0g1iLZ.png)

## API Reference

### Check License

```http
POST /check
```

#### Body

| Key       | Type     | Description                                                     |
|:----------|:---------|:----------------------------------------------------------------|
| `key`     | `string` | **Required**. Your base64 encrypted license key                 |
| `product` | `string` | **Required**. The product the license is for                    |
| `hwid`    | `string` | **Required**. The base64 encrypted hardware id of the requester |

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
  "expires": "2023-06-02T06:00:47.270+00:00"
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