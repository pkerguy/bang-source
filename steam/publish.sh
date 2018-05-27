echo "Injecting path variables...."
export PATH=$PATH:/opt/steamcmd
echo "Done"
steamcmd.sh +login yourfunworldstudiosbuildci sVgj3r6MvfFn5IL +run_app_build_http "../scripts/app_build_1000.vdf" +quit
