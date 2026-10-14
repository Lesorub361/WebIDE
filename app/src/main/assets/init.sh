#!/bin/sh
#
# WebIDE - A powerful IDE for Android web development.
# Copyright (C) 2025  如日中天  <3382198490@qq.com>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.
#

# 只设置当前终端进程的环境；不要把 Android 动态库路径传给 Alpine 程序。
unset LD_LIBRARY_PATH LD_PRELOAD
export PATH=/usr/local/bin:/usr/bin:/bin:/usr/local/sbin:/usr/sbin:/sbin
export HOME=/root

# 非交互命令直接执行，避免欢迎语污染输出。
if [ "$#" -gt 0 ]; then
    exec "$@"
fi

# 欢迎语输出
if [ -x /usr/bin/apt ]; then
cat <<'EOF'
  _       __     __    ________  ______ 
 | |     / /__  / /_  /  _/ __ \/ ____/ 
 | | /| / / _ \/ __ \ / // / / / __/    
 | |/ |/ /  __/ /_/ // // /_/ / /___    
 |__/|__/\___/_.___/___/_____/_____/    
 
 Welcome to WebIDE Ubuntu 20.04 LTS Subsystem (PRoot)
 
 Package Management:
   - apt update && apt install <pkg>
   - npm install -g <package>
   - pip install <module>

 AI Coding Agents:
   - Claude Code: claude
   - Antigravity: agy
   - DeepSeek Harness: dsh
EOF
else
cat <<'EOF'
Welcome to WebIDE Terminal!
Installing : apk add <pkg>
Updating   : apk update && apk upgrade
EOF
fi

cd / || exit 1
if [ -x /usr/bin/bash ]; then
    export SHELL=/usr/bin/bash
    export PS1='\[\033[01;32m\]root@ubuntu\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\$ '
    exec /usr/bin/bash -i
elif [ -x /bin/bash ]; then
    export SHELL=/bin/bash
    export PS1='root@localhost:\w\$ '
    exec /bin/bash -i
else
    export SHELL=/bin/ash
    export PS1='root@localhost:\w\$ '
    exec /bin/ash -i
fi
