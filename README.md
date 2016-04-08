# Akka & Docker

```bash
sbt docker:publishLocal
docker run --name seed-1 akka-docker:2.3.4 --seed
docker run --name seed-2 akka-docker:2.3.4 --seed <ip-of-your-seed-1>:2551
docker run --name node-1 akka-docker:2.3.4 <ip-of-your-seed-1>:2551 <ip-of-your-seed-2>:2551
docker run --name node-2 akka-docker:2.3.4 <ip-of-your-seed-1>:2551 <ip-of-your-seed-2>:2551
```

