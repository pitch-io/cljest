all:

install:
	npm i

clean:
	rm -rf .jest
	rm -rf .shadow-cljs

lint: install
	clojure -A:fmt -M -m nsorg.cli src
	clojure -A:fmt -M -m cljfmt.main check src
	./node_modules/.bin/eslint .
	./node_modules/.bin/prettier --check .

lint-fix: install
	clojure -A:fmt -M -m nsorg.cli --replace src
	clojure -A:fmt -M -m cljfmt.main fix src
	./node_modules/.bin/eslint --fix .
	./node_modules/.bin/prettier -w .

publish:
	clj -A:publish -M -m deps-library.release

server:
	clojure -A:test -X cljest.compilation/watch

compile:
	clojure -A:test -X cljest.compilation/compile

watch: install
	./node_modules/.bin/jest --watch

jest-ci: install compile
	CI=true ./node_modules/.bin/jest --ci

repl:
	clj -J-Dclojure.server.repl="{:port 5678 :accept clojure.core.server/repl}"
