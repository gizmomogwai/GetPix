puts "pngs: #{Dir.glob('**/*.png').inject(0) {|sum, f|sum + File.size(f)}}"
jars = Dir
  .glob('**/*.jar')
  .map{|jar|[jar, File.size(jar)]}
  .sort{|a,b|b[1] <=> a[1]}
  .map{|i| "#{i[0]}: #{i[1]}"}
  .join("\n")
puts "jars: #{jars}"
