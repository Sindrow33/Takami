#!/bin/sh
cd "$(dirname "$0")" || exit 1
FAIL=0
ok()  { printf '  [ OK ] %s\n' "$1"; }
bad() { printf '  [FAIL] %s\n' "$1"; FAIL=1; }

KT=$(find . -name '*.kt' -not -path './*/build/*' | sort)
[ -n "$KT" ] || { echo "нет ни одного .kt файла"; exit 1; }
N=$(echo "$KT" | wc -l | tr -d ' ')

echo "== 1. количество файлов =="
if [ "$N" -eq 18 ]; then ok "18 из 18"; else bad "ожидалось 18, найдено $N"; fi

echo "== 2. целостность (файл не обрезан) =="
for f in $KT; do
  [ -s "$f" ] || { bad "$f пустой"; continue; }
  case "$(tail -c 4 "$f" | tr -d ' \n\t')" in
    *'}'|*')') ;;
    *) bad "$f обрезан, последняя строка: $(tail -n 1 "$f")" ;;
  esac
  grep -qE '<<<<<<<|>>>>>>>' "$f" && bad "$f: маркеры конфликта"
done
[ $FAIL -eq 0 ] && ok "все файлы дописаны до конца"

echo "== 3. баланс скобок =="
for f in $KT; do
  awk -v F="$f" '
    { n=length($0)
      for(i=1;i<=n;i++){ c=substr($0,i,1)
        if(c=="{")b++; else if(c=="}")b--
        else if(c=="(")p++; else if(c==")")p--
        else if(c=="[")s++; else if(c=="]")s-- } }
    END{ if(b!=0||p!=0||s!=0) printf "  [FAIL] %s  {}=%d ()=%d []=%d\n",F,b,p,s }' "$f"
done > /tmp/br.txt
if [ -s /tmp/br.txt ]; then cat /tmp/br.txt; FAIL=1; else ok "скобки сбалансированы"; fi

echo "== 4. package совпадает с каталогом =="
for f in $KT; do
  pkg=$(grep -m1 '^package ' "$f" | awk '{print $2}')
  [ -z "$pkg" ] && { bad "$f без package"; continue; }
  dir=$(dirname "$f" | sed 's#.*/src/main/kotlin/##;s#.*/src/test/kotlin/##' | tr '/' '.')
  [ "$pkg" = "$dir" ] || bad "$f: package '$pkg' не равен '$dir'"
done
[ $FAIL -eq 0 ] && ok "раскладка пакетов верна"

echo "== 5. тройные кавычки парные =="
for f in $KT; do
  cnt=$(grep -o '"""' "$f" | wc -l | tr -d ' ')
  [ $((cnt % 2)) -eq 0 ] || bad "$f: нечётное число тройных кавычек ($cnt)"
done
[ $FAIL -eq 0 ] && ok "raw-строки закрыты"

echo "== 6. границы модулей =="
if grep -rqE '^import okhttp3' imagesearch-resolver/src 2>/dev/null; then
  bad "resolver тянет okhttp3, хотя не должен знать про сеть"
elif grep -rqE '^import moe\.scenesearch\.(core|resolver)' imagesearch-api/src 2>/dev/null; then
  bad "api зависит от core или resolver: циклическая зависимость"
else
  ok "зависимости однонаправленные, core и resolver смотрят в api"
fi

echo "== 7. состав по модулям =="
for m in imagesearch-api imagesearch-core imagesearch-resolver; do
  c=$(find "$m" -name '*.kt' 2>/dev/null | wc -l | tr -d ' ')
  printf '  %-22s %s файлов\n' "$m" "$c"
done
echo "  строк кода: $(cat $KT | wc -l | tr -d ' ')"

echo
if [ $FAIL -eq 0 ]; then
  echo "ВСЁ ЧИСТО. Типы и импорты проверит CI после пуша."
else
  echo "ЕСТЬ ОШИБКИ, см. выше."
fi
exit $FAIL
