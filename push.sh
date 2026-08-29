#!/bin/sh
cd ~/Takami || exit 1
export GIT_TERMINAL_PROMPT=0
C="$PWD/.git/.ghc"
[ -s "$C" ] || { echo "нет .git/.ghc — токен не записан"; exit 1; }
git -c user.name=Sindrow33 -c user.email=sindrow33@users.noreply.github.com \
    add -A
git -c user.name=Sindrow33 -c user.email=sindrow33@users.noreply.github.com \
    commit -m "${1:-правки прототипа}" || echo "нечего коммитить"
git -c credential.helper="store --file=$C" push origin main \
  && echo "→ https://sindrow33.github.io/Takami/prototype/hub.html"
