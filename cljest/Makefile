all:

install:
	npm i

lint: install
	clojure -M -m nsorg.cli src
	clojure -M -m cljfmt.main check src
	./node_modules/.bin/eslint .
	./node_modules/.bin/prettier --check .

publish:
	clj -M -m deps-library.release

server:
	clojure -X cljest.compilation/watch

compile:
	clojure -X cljest.compilation/compile

watch: install
	./node_modules/.bin/jest --watch

jest-ci: install compile
	CI=true ./node_modules/.bin/jest --ci

repl:
	clj -J-Dclojure.server.repl="{:port 5678 :accept clojure.core.server/repl}"
