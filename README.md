# Akka & Docker

```bash
sbt docker:publishLocal
docker run --name backend anistal-akka-cluster:0.1 --backend rh=<redisHost>,rp=<redisPort>ck=<customerKey>,cs=<customerSecret>,at=<accessToken>,ats=<accessTokenSecret>
docker run --name frontend anistal-akka-cluster:0.1 --frontend seed=<backendHost>,ck=<customerKey>,cs=<customerSecret>,at=<accessToken>,ats=<accessTokenSecret>
 
```

