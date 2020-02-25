require 'sinatra'
require 'rest-client'

set :public_folder, File.dirname(__FILE__) + '/src/main/resources'
set :api_url, "http://localhost:8000"

def self.any (url, &block)
  get(url, &block)
  post(url, &block)
  put(url, &block)
  delete(url, &block)
end

get '/' do
  serve("index.html")
end

get '/js/:name' do
  content_type 'application/javascript'
  serve("javascript/#{params['name']}")
end

get '/css/:name' do
  content_type 'application/css'
  serve("css/#{params['name']}")
end

any '/api/*' do
  # todo send body
  path = params["splat"].first
  http_method = request.env["REQUEST_METHOD"]
  url = settings.api_url
  req = RestClient::Request.execute(
    method: http_method,
    url: "#{url}/api/#{path}"
  )
  content_type 'application/json'
  req.body
end

get '/*' do
    redirect('/')
end

def serve(name)
   File.read("#{settings.public_folder}/#{name}")
end