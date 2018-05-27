chmod -R +x builder
chmod -R +x builder_linux
chmod -R +x builder_osx
./builder_linux/steamcmd.sh +login yourfunworldstudiosbuildci sVgj3r6MvfFn5IL +run_app_build_http "../scripts/app_build_1000.vdf" +quit
