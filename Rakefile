require 'rake'

task :compile do
  p "regenerate truerss.js"
  c_to = "#{Dir.pwd}/src/main/resources/javascript"
  path = "#{Dir.pwd}/src/main/resources/javascript/app"
  files = ["ext", "feeds_controller", "ws_controller",
    "contoller_ext", "main_controller", "plugins_controller", "models", "sources_controller",
    "settings_controller", "upload_controller", "search_controller",
    "about_controller",
    "templates", "app"
   ].map{|f| "#{path}/#{f}.coffee"}.join(" ")
  file = "#{c_to}/truerss.js"
  p "write #{file}"

  system("cat #{files} | coffee -c -b --stdio > #{c_to}/truerss.js")
end

task :default => :compile
