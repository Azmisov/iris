# IRIS

![](https://github.com/mnit-rtmc/iris/workflows/compile/badge.svg)
![](https://github.com/mnit-rtmc/iris/workflows/docs/badge.svg)

IRIS is an advanced traffic management system.  For more information, look
[here](https://mnit-rtmc.github.io/iris/index.html).

## Development with Visual Studio Code
The easiest thing will be to first do a [normal install](https://mnit-rtmc.github.io/iris/installation.html).
This will ensure dependencies are installed, database setup, users created, etc. Then, run the following:

```console
root@server:~$ ./bin/iris_ctl dev
```

This creates a `./dev` folder to hold the development runtime configuration and logs. It copies the
`./etc/dev` templates, as well as generated files/passwords from the install. You can take a look at
`iris_ctl` and perform the step manually if desired. The reason for copying from `./etc/dev` to
`./dev` is so you can customize the configuration without it being tracked in source control.

The `.vscode/launch.json` is configured to run the Iris server in development mode. It uses the
configuration inside the `./dev/` folder. Main server logs will use stdout/stderr (the
`runInEclipse` option), while child component logs will get output to various places within
`./dev/logs/`.

The class and source paths should be autodetected from the Java Project. Install the recommended
Java extension pack for VSCode and open `iris.code-workspace`. In the `Java Projects` view, you'll
see it doing an initial analysis of the code. You may need to manually change the JDK runtime
setting and restart VSCode. You'll also may need to install `openjdk-source` and `openjdk-javadoc`
to get intellisense working.

If you're using a different IDE, or the Java Project is not working properly, you may set the source
and class path manually. Copy the values from `.vscode/settings.json`.