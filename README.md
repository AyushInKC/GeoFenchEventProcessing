Geofence Event Processing — Backend

Spring Boot + MongoDB
Internship assignment for Text Anything — backend service that accepts vehicle GPS events, detects geofence (zone) enter/exit transitions, stores state in MongoDB and exposes simple APIs.

This README explains how to build and run the backend using Docker, how to seed MongoDB with sample data, the API surface (requests + example responses), the geofence logic and assumptions, operational notes, troubleshooting and suggested improvements.

Table of contents

What this project does (short)

Prerequisites

Files of interest

Build & Run with Docker (quick)

Run with Docker Compose (recommended for local dev)

Seed MongoDB (sample zone + vehicle data)

API endpoints — request & response examples

Geofence logic & design decisions

Logging, monitoring & healthchecks

Troubleshooting / debug tips

What to improve (if you had more time)

Contact / notes for reviewers

<a name="what"></a>

1. What this project does (short)

Provides an HTTP API to receive vehicle location events (POST /api/events).

Determines which geofence zones a vehicle is inside (rectangle bounding boxes).

Compares the new zone set with last-known zones for the vehicle and emits enter/exit events.

Persists current vehicle state and a history of zone events in MongoDB.

Provides APIs to query current vehicle state and event history.

<a name="prereq"></a>

2. Prerequisites

Docker (v20+) and Docker Compose (if you use docker-compose example).

A machine with at least 2 GB free disk and network access to MongoDB (local or Atlas).

If you plan to use a remote MongoDB Atlas cluster, ensure your URI is accessible from the machine where Docker runs.

<a name="files"></a>

3. Files of interest in the repo

Dockerfile — multi-stage build for the Spring Boot app

pom.xml — Maven project configuration

src/ — Java source (controllers, services, models, repositories)

docker-compose.yml — suggested compose file (example included below)

seed/ — sample JSON files with zones and vehicles (examples in README)

README.md — this file

Note on the Dockerfile you provided
Your Dockerfile uses a two-stage build: FROM openjdk:21-jdk AS build to run Maven (via ./mvnw) and FROM openjdk:17-jdk for the runtime image. That will work, but keep the runtime JDK version consistent with your build environment if you rely on newer language features. The Dockerfile below mirrors what you submitted.

<a name="docker-quick"></a>

4. Build & Run with Docker (quick)
4.1. Build the Docker image

From the project root (where the Dockerfile and pom.xml are):

# Make sure mvnw is executable
chmod +x ./mvnw

# Build image (this uses your Dockerfile)
docker build -t geofence-backend:latest .

4.2. Run the container (local Mongo)

If you run a local MongoDB on mongodb://host.docker.internal:27017/GeoFence (Docker on Linux may use localhost), pass the connection string:

docker run -e SPRING_DATA_MONGODB_URI="mongodb://host.docker.internal:27017/GeoFence" \
  -e SPRING_DATA_MONGODB_DATABASE="GeoFence" \
  -p 8080:8080 \
  --name geofence-backend \
  geofence-backend:latest

4.3. Run the container (MongoDB Atlas)

If you want to connect to an Atlas cluster, set SPRING_DATA_MONGODB_URI. Example:

docker run -e SPRING_DATA_MONGODB_URI="mongodb+srv://<user>:<pass>@cluster0.mongodb.net/GeoFence?retryWrites=true&w=majority" \
  -e SPRING_DATA_MONGODB_DATABASE="GeoFence" \
  -p 8080:8080 \
  --name geofence-backend \
  geofence-backend:latest


Important: The Spring property key the app expects is the standard spring.data.mongodb.uri — when passing into Docker container with environment variables, use the environment variable SPRING_DATA_MONGODB_URI. Spring Boot automatically maps SPRING_DATA_MONGODB_URI → spring.data.mongodb.uri. You may also set SPRING_DATA_MONGODB_DATABASE if necessary.

<a name="docker-compose"></a>

5. Docker Compose (recommended for local dev)

Create a docker-compose.yml (example):

version: '3.8'
services:
  mongo:
    image: mongo:6.0
    restart: unless-stopped
    ports:
      - "27017:27017"
    volumes:
      - mongo-data:/data/db

  geofence:
    image: geofence-backend:latest
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATA_MONGODB_URI=mongodb://mongo:27017/GeoFence
      - SPRING_DATA_MONGODB_DATABASE=GeoFence
    depends_on:
      - mongo

volumes:
  mongo-data:


Use:

# build & run
docker-compose up --build -d
# view logs
docker-compose logs -f geofence


<a name="seed"></a>

6. Seed MongoDB (sample zone & vehicle data)

You need zones collection documents for the geofences. Below are two options: readable IDs (recommended) or auto-generated Mongo _id. Use whichever you prefer.

Option 1 — Insert readable _id (recommended)

This stores zone ID as Mongo _id and makes API responses show friendly zone ids:

Create a zones.json file with:

[
  {
    "_id": "zone001",
    "name": "North Yard",
    "minLat": 25.3201,
    "maxLat": 25.325,
    "minLon": 82.9901,
    "maxLon": 82.995
  },
  {
    "_id": "zone002",
    "name": "Warehouse Zone A",
    "minLat": 25.31,
    "maxLat": 25.3155,
    "minLon": 82.98,
    "maxLon": 82.986
  },
  {
    "_id": "zone003",
    "name": "Parking Area",
    "minLat": 25.3302,
    "maxLat": 25.3338,
    "minLon": 82.9704,
    "maxLon": 82.9759
  }
]


Import with mongoimport (if Mongo is running locally):

# If using local mongo and docker-compose above:
docker exec -i $(docker ps -qf "name=mongo") \
  mongoimport --db GeoFence --collection zones --jsonArray --drop --file /path/on/host/zones.json


If using mongo CLI directly:

mongoimport --uri "mongodb://localhost:27017/GeoFence" --collection zones --file zones.json --jsonArray --drop

Option 2 — Insert documents without _id (Mongo generates ObjectId)

If you use your current Zone model that maps @Id private String id; and you want Mongo to auto-generate _id, insert documents without _id (then the API returns Mongo ObjectIds as zone IDs). Not recommended for human-friendly output.

Example vehicle seed (optional)

Create vehicles.json:

[
  {
    "vehicleId": "VHC001",
    "lat": 0,
    "lon": 0,
    "lastTimestamp": "",
    "currentZones": []
  }
]


Import similarly with mongoimport. The backend will also create vehicles on first event if not present.

<a name="api"></a>

7. API endpoints — preview, sample requests & responses

Base URL (local): http://localhost:8080

All APIs use JSON.

1) POST /api/events — accept a GPS location event

Description: Accept vehicle location event, compute enter/exit, persist state & events.

Request

POST /api/events
Content-Type: application/json

{
  "vehicleId": "VHC001",
  "lat": 25.332,
  "lon": 82.973,
  "timestamp": "2025-11-27T10:10:00Z"
}


Successful Response

{
  "vehicleId": "VHC001",
  "entered": ["zone003"],
  "exited": [],
  "currentZones": ["zone003"]
}


Notes:

entered contains zone IDs the vehicle just entered in this event.

exited contains zone IDs the vehicle just exited in this event.

currentZones is the list after processing this event.

The service ignores events with timestamps older than the vehicle's lastTimestamp.

2) GET /api/vehicles/{vehicleId} — current vehicle state

Request

GET /api/vehicles/VHC001


Response

{
  "id": "<mongodb _id>",
  "vehicleId": "VHC001",
  "lat": 25.332,
  "lon": 82.973,
  "lastTimestamp": "2025-11-27T10:10:00Z",
  "currentZones": ["zone003"]
}

3) GET /api/zones/events/{vehicleId} — history of enter/exit

Request

GET /api/zones/events/VHC001


Response (example)

[
  {
    "vehicleId": "VHC001",
    "zoneId": "zone003",
    "transition": "ENTER",
    "timestamp": "2025-11-27T10:10:00Z"
  }
]


<a name="logic"></a>

8. Geofence logic & decisions (implementation notes)

Zone representation: rectangular bounding boxes stored as minLat, maxLat, minLon, maxLon.

Point-in-zone check: minLat <= lat <= maxLat and minLon <= lon <= maxLon. This is fast and appropriate for small/rectangular zones.

Timestamps & ordering: Each event includes timestamp. The service ignores events older than the vehicle’s stored lastTimestamp to avoid out-of-order updates.

Transition logic:

prev = vehicle.currentZones

new = zonesContainingPoint

entered = new - prev

exited = prev - new

IDs returned: By default the code maps the Mongo _id field to the id property in Zone model, so currentZones will contain Mongo document IDs. To return custom zone names/IDs, either:

Insert zones with readable _id values (zone001) OR

Modify the Zone model to have separate zoneCode field and return that from Zone::getId().

<a name="ops"></a>

9. Logging, monitoring & healthchecks (operational awareness)

The service logs:

Zones loaded at startup (count + bounds)

Each incoming event with lat/lon/timestamp

Per-zone checks and whether vehicle is inside

Saved enter/exit events and vehicle state updates

Use log aggregation (ELK / Splunk) in production.

Add Prometheus metrics (counters for events received, events dropped, transitions saved, processing latency).

Add /actuator/health (Spring Actuator) to enable Kubernetes health checks (optional, add Actuator dependency).

<a name="troubleshoot"></a>

10. Troubleshooting / common issues

DEBUG → Loaded Zones Count = 0

Check spring.data.mongodb.uri and spring.data.mongodb.database env variables. If wrong, Spring connects to an empty DB.

Verify collection name: model uses @Document("zones") → collection must be zones.

Use mongo or Compass to confirm db.zones.find().pretty().

API returns empty currentZones

Verify the coordinates are inside the zone bounds.

Confirm zone bounds in DB and test with sample coordinates in README section.

Check logs — debug mode prints per-zone inside/outside checks.

IDs returned are Mongo ObjectIds, not human IDs

Either insert zones with readable _id values (see Seed section) or alter Zone model to expose a custom code field.

Event rejected due to old timestamp

Verify lastTimestamp stored in vehicle document vs timestamp you send — service rejects older timestamps.

<a name="improve"></a>

11. What you would improve given more time (short prioritized list)

Switch to polygon / PostGIS — polygonal zones stored in Postgres/PostGIS or GeoJSON in Mongo with spatial queries for realistic geofences (not just rectangles). Or use JTS/EPSG transforms to support real area shapes.

Debounce / hysteresis — implement temporal/spatial smoothing (temporal hold for N seconds or buffered polygons) to avoid rapid enter/exit flickering due to GPS noise.

Asynchronous event pipeline — publish transition events to a message queue (Kafka/RabbitMQ) for downstream consumers (notifications, billing).

Auth & rate limiting — API keys / JWT and per-client rate limits.

Metrics & distributed tracing — Prometheus + Grafana + OpenTelemetry for observability.

Automated tests & CI/CD — unit and integration tests, GitHub Actions to run tests and publish Docker images.

Frontend map UI — React + Mapbox or Leaflet to visualize zones and live vehicle tracking.

<a name="contact"></a>

12. Contact / notes for reviewers

This backend was created and tuned as an internship assignment for Text Anything.

If you want me to:

Return friendly zone IDs (e.g., zone003) instead of Mongo ObjectId, say “use readable zone IDs” and I will update the Zone model and migration instructions.

Provide a Postman collection or a simple React demo for live testing — I can add those next.

Quick reference commands
# Build locally (uses included mvnw)
chmod +x ./mvnw
./mvnw clean package -DskipTests
docker build -t geofence-backend:latest .

# Run with local Mongo:
docker run -e SPRING_DATA_MONGODB_URI="mongodb://host.docker.internal:27017/GeoFence" \
  -e SPRING_DATA_MONGODB_DATABASE="GeoFence" \
  -p 8080:8080 geofence-backend:latest

# Or using docker-compose
docker-compose up --build -d

# Seed zones (example)
mongoimport --uri "mongodb://localhost:27017/GeoFence" --collection zones --file zones.json --jsonArray --drop

# Test API
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{"vehicleId":"VHC001","lat":25.332,"lon":82.973,"timestamp":"2025-11-27T10:10:00Z"}'


If you want, I can:

produce a docker-compose.yml in the repo, ready-to-run,

generate zones.json and vehicles.json files and place them into a seed/ folder,

or provide a small Postman collection.

Tell me which one and I’ll create it.

You said:
How do i generate the Jar file
ChatGPT said:

To generate the JAR file for your Spring Boot project, you have two options:

✅ Option 1 — Using Maven Wrapper (recommended)

Your project already includes:
✔ mvnw
✔ .mvn/ folder
✔ pom.xml

So just run:

Linux / macOS
chmod +x mvnw
./mvnw clean package -DskipTests

Windows (PowerShell or CMD)
mvnw.cmd clean package -DskipTests

📌 Where is the JAR generated?

After the build completes, check:

/target/your-app-name-1.0.0.jar


For example:

target/GeofenceEventProcessing-0.0.1-SNAPSHOT.jar


This is the JAR Docker needs.

✅ Option 2 — Using system Maven (if installed)

If you have Maven installed globally:

mvn clean package -DskipTests

🚀 Run the JAR manually (optional test)

After building:

java -jar target/*.jar


If port 8080 is free, the backend will start.

⚠️ Common Build Issues
❌ Error: mvnw not executable

Run:

chmod +x mvnw

❌ Error: JAVA_HOME not set

Install Java 17 or 21, then:

export JAVA_HOME=/path/to/java

❌ Error: Tests failing

Skip tests:

./mvnw clean package -DskipTests
