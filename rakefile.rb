def mobile
  '10.10.12.243:4567'
end
desc 'show images on getpix'
task :index do
  sh "http #{mobile}/index"
end

desc 'post files something with xform data'
task :post do
  sh "http -v -f #{mobile}/files/test suffix=123 filename=test"
end
