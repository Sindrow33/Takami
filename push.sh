#!/bin/sh
cd ~/Takami || exit 1
export GIT_TERMINAL_PROMPT=0
git add -A
git commit -m "${1:-правки прототипа}" || echo "нечего коммитить"
git push origin main && echo "→ https://sindrow33.github.io/Takami/prototype/hub.html"
