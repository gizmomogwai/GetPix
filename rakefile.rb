def mobile
#  '192.168.1.125:4567'
#  '192.168.44.1:4567'
#  "192.168.1.231:4567"
  "android.local:4567"
end

desc 'show images on getpix'
task :index do
  sh "http #{mobile}/index"
end

desc "get one image from getpix"
task :get do
  sh "curl #{mobile}/files//storage/emulated/0/DCIM/Camera/PXL_20231019_141333628.jpg > out.jpg"
end

desc 'post files something with xform data'
task :post do
  sh "http -v -f #{mobile}/files/test suffix=123 filename=test"
end

def project_root
  Dir.pwd
end

def docker_image
  "android-dev:0.0.1"
end

def prepare_environment
  sh "brew install android-commandlinetools"
  sh 'sdkmanager --install --sdk_root=$HOME/.android "build-tools;30.0.3"'
  sh 'sdkmanager --install --sdk_root=$HOME/.android "platforms;30.0.3"'
end

namespace :docker do
  desc "Build docker image"
  task :build_image do
    sh "podman build . --tag #{docker_image}"
  end

  desc "Build project"
  task :build_project do
    sh "podman run --rm --interactive --tty --mount type=bind,source=#{project_root},target=/ws #{docker_image}"
  end
end
