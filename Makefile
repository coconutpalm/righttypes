jar:
	clojure -X:jar :version '"0.7.0"'

deploy:
	clj -X:deploy

ALL: jar deploy
