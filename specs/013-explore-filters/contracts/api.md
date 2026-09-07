# API Contracts: Filtros avanzados de Explorar

**Date**: 2026-09-07
**Feature**: 013-explore-filters

## Endpoints Used

All endpoints are public (no authentication required).

### GET /stations

Search stations with filters and pagination.

**Query Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| name | string | No | Filter by station name (partial match) |
| country | string | No | Filter by country name |
| language | string | No | Filter by language |
| tag | string | No | Filter by genre/tag |
| limit | integer | No | Results per page (1-100, default 24) |
| offset | integer | No | Pagination offset (default 0) |
| unique | boolean | No | Return only unique stations (default false) |

**Response** (200 OK):

```json
{
  "items": [
    {
      "id": "uuid-string",
      "name": "Station Name",
      "url": "https://stream.example.com/radio",
      "homepage": "https://station.example.com",
      "favicon": "https://station.example.com/icon.png",
      "country": "Spain",
      "countryCode": "ES",
      "language": "Spanish",
      "tags": ["rock", "pop"],
      "codec": "MP3",
      "bitrate": 128,
      "isSsl": true,
      "lastCheckOk": true,
      "votes": 100,
      "clickCount": 500
    }
  ],
  "pagination": {
    "offset": 0,
    "limit": 24,
    "hasMore": true
  }
}
```

### GET /stations/countries

Get list of available countries.

**Response** (200 OK):

```json
{
  "items": ["Spain", "France", "Germany", "United States", ...]
}
```

### GET /stations/languages

Get list of available languages.

**Response** (200 OK):

```json
{
  "items": ["Spanish", "English", "French", "German", ...]
}
```

### GET /stations/tags

Get list of available tags/genres.

**Response** (200 OK):

```json
{
  "items": ["rock", "pop", "jazz", "classical", "news", ...]
}
```

## Error Responses

All endpoints may return:

| Status | Description |
|--------|-------------|
| 400 | Invalid query parameters |
| 500 | Internal server error |
| 503 | Service unavailable (RadioBrowser down) |

**Error Response Body**:

```json
{
  "error": "Error message description"
}
```

## Client Behavior

1. **Filter Lists**: Load once on screen open, cache in memory for session
2. **Search**: Trigger on "Buscar" button press, not on filter change
3. **Pagination**: Use "Cargar más" button with offset increment
4. **Degraded Mode**: If filter list endpoint fails, allow manual text input
5. **Offline**: If search fails and cache exists, show cached results with "offline" indicator
