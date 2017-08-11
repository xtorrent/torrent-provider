# torrent-provider
gko3 torrent-provider service

# usage
gko3 down --source gko3://hostNameOrIp/path/to/file [other gko3 OPTIONS]


### compile

```
docker pull csuliuming/gko3-compile-env:v1.0
docker run -ti -name xtorrent csuliuming/gko3-compile-env:v1.0 bash
git clone https://github.com/xtorrent/torrent-provider.git
cd torrent-provider
./build.sh                # generate gko3-provider.tgz in $PWD/
```

### run
- must set `JAVA_HOME` before start torrent-provider service
- start & stop

    ```
    bin/control start
    bin/control stop
    ```