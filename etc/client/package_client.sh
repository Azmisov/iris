# The JAR's we've packaged with IRIS are not modular jars. Unfortunately, jlink
# doesn't work with automatic modules, which makes sense; automatic modules
# include the entire classpath, so there is no optimized JRE build possible.
# We convert them to modular jars using this technique:
# https://stackoverflow.com/a/47222302/379572
mkdir -p ./build/modules/
jdeps --ignore-missing-deps --generate-module-info ./build/modules/ ./iris-client/lib/*
jdeps_success=$?
if [ $jdeps_success -eq 0 ]; then
	# so no easy way to programmatically get the module name that was generated by
	# jdeps; seems you'd need to extract MANIFEST.MF and then find
	# Automatic-Module-Name, or else do some automatic name resolution if that
	# attribute doesn't exist; for now, I'll just hardcode the names; see
	# https://stackoverflow.com/questions/7066063
	modules=(
		# jar path, module name
		# modularizing in this order avoids javac warnings about transitive automatic modules
		'./iris-client/lib/javax.activation-@@JAVAX.VERSION@@.jar' 'java.activation'
		'./iris-client/lib/jna-@@JNA.VERSION@@.jar' 'com.sun.jna'
		'./iris-client/lib/jna-platform-@@JNA.VERSION@@.jar' 'com.sun.jna.platform'
		'./iris-client/lib/json-@@JSON.VERSION@@.jar' 'org.json'
		'./iris-client/lib/mail.jar' 'java.mail'
		'./iris-client/lib/postgis-geometry-@@POSTGIS.VERSION@@.jar' 'postgis.geometry'
		'./iris-client/lib/gst1-java-core-@@GST.JAVA.VERSION@@.jar' 'org.freedesktop.gstreamer'
		'./iris-client/lib/iris-common-@@VERSION@@.jar' 'iris.common'
		'./iris-client/lib/iris-client-@@VERSION@@.jar' 'iris.client'
	)
	for ((i=0; i<${#modules[@]}; i+=2)); do
		jar=${modules[$i]}
		name=${modules[$i+1]}
		echo Modularizing $jar
		# compile module-info.java for this jar
		javac --module-path ./iris-client/lib/ --patch-module $name=$jar ./build/modules/$name/module-info.java
		# inject into jar
		jar uf $jar -C ./build/modules/$name module-info.class
		# can run the following to verify it has been modularized:
		#jar --file=$jar --describe-module
	done
else
	echo 'Error code from jdeps; assuming JARs have already been modularized'
fi

echo "Packaging..."
jpackage \
	--name iris-client \
	--app-version @@VERSION@@ \
	--icon ./iris-client/images/iris_icon.png \
	--input iris-client/lib/ \
	--main-jar 'iris-client-@@VERSION@@.jar' \
	--main-class us.mn.state.dot.tms.client.MainClient \
	--arguments 'http://@@WEBSTART.HOST@@/iris-client/iris-client.properties' \
	--java-options -Xmx1024m \
	--type rpm \
	--dest iris-client/ \
	--description 'Intelligent Roadway Information System (IRIS) desktop client interface' \
	--about-url 'http://@@WEBSTART.HOST@@/iris-client/index.html' \
	--win-help-url 'http://@@WEBSTART.HOST@@/iris-client/index.html' \
	--linux-app-category 'Applications/Engineering' \
	--license-file './LICENSE' \
	--linux-rpm-license-type 'GPLv2+' \
	--win-shortcut-prompt \
	--linux-shortcut \
