Morrigan Media Player
=====================

A media player and organiser with written in Java using OSGi.  For project background [see this blog post](https://medium.com/@haku/9-years-of-hacking-still-searching-for-the-perfect-media-player-1e84046c7dad)

Build
-----

`mvn clean package`

Known Issues
------------

* **OSX**: Requires some manual setup: [[Morrigan on OSX]].  Also not well tested.  Bug reports appreciated.
* **Ubuntu 11.04**  [https://code.google.com/p/gstreamer-java/issues/detail?id=76 Issue 76: Gstreamer can't be loaded in Ubuntu 11.04].  Running `sudo apt-get install libgtk2.0-dev` seems to resolve this.  (fixes "NoClassDefFoundError: Could not initialize class org.gstreamer.lowlevel.GObjectAPI")
* **Ubuntu 12.10 x84_64** `aptitude install vlc libvlc-dev libvlccore-dev`

Licence
-------

This source code is part of Project Morrigan and is copyrighted by Fae Hutter 2010 to 2017.  All rights reserved.

This source code is made available under the Apache 2 licence.
This copy of the source code should also contain LICENCE and NOTICE files which contain the full licence terms copyright notices respectfully.

The full licence can also be found here:
http://www.apache.org/licenses/LICENSE-2.0

This repository contains software from third-parties that are licensed, distributed and owned by their respective owners.  For details see THIRDPARTY file.  Provenance of third-party software is recorded in provenance.txt files.

Libraries
---------

| [Rich Client Platform](http://wiki.eclipse.org/index.php/Rich_Client_Platform)        | Eclipse Public License.                                              |
| [sqlite-jdbc](https://code.google.com/p/sqlite-jdbc)                                  | The Apache Software License, Version 2.0.                            |
| [jetty](http://jetty.codehaus.org/jetty/)                                             | The Apache Software License, Version 2.0.                            |
| [Direct Show Java Wrapper (Windows only)](http://www.humatic.de/htools/dsj.htm)      | The library is free for use in non-commercial projects.              |
| [gstreamer-java (Linux only)](http://code.google.com/p/gstreamer-java)               | GNU Lesser General Public License.                                   |
| [jintellitype (Windows only)](http://melloware.com/products/jintellitype/index.html) | The Apache Software License, Version 2.0.                            |
| [jxgrabkey (Linux only)](http://sourceforge.net/projects/jxgrabkey)                  | GNU Library or "Lesser" General Public License version 3.0 (LGPLv3). |
| [vlcj (OSX only)](http://code.google.com/p/vlcj/)                                    | GNU GPL v3                                                           |
| [ jaudiotagger](http://www.jthink.net/jaudiotagger/)                                  | [http://www.gnu.org/copyleft/lesser.html LGPL]                       |
