Morrigan Media Player
=====================

A media player and organiser, 15+ years in the making and still going.  For project background [see this blog post](https://medium.com/@haku/9-years-of-hacking-still-searching-for-the-perfect-media-player-1e84046c7dad)

Build and Run
-------------

```shell
$ mvn clean package
$ java -jar target/morrigan-1-SNAPSHOT-jar-with-dependencies.jar --http 28080 --ssh 14022 --dlna
```

External Dependences
--------------------

The compiled-in player depends on VLC.  On Debian / Ubuntu try `apt install vlc libvlc-dev libvlccore-dev`.  See https://github.com/caprica/vlcj for more.

Transcoding require ffmpeg (and ffprobe) to be installed and on PATH.

Licence
-------

This source code is part of Project Morrigan and is copyrighted by Fae Hutter 2010 to 2022.  All rights reserved.

This source code is made available under the Apache 2 licence.
This copy of the source code should also contain LICENCE and NOTICE files which contain the full licence terms copyright notices respectfully.

The full licence can also be found here:
http://www.apache.org/licenses/LICENSE-2.0

Some branches in this repository contains software from third-parties that are licensed, distributed and owned by their respective owners.  For details see THIRDPARTY file in those branches.  Provenance of third-party software is recorded in provenance.txt files.
