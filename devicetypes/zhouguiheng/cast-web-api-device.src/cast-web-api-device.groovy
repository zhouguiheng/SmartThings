/**
 *  Cast Web API Hubitat Driver
 *
 *  Copyright 2020 Vincent Zhou
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

metadata {
	preferences {
		input "apiHost", "string", title: "Cast Web API Hostname/IP (https://vervallsweg.github.io/cast-web/installation)", multiple: false, required: true
		input "apiPort", "number", title: "Cast Web API Port", multiple: false, required: true
		input "deviceId", "string", title: "Device ID (Check http://apiHost:apiPort/device)", required: true
		input "googleTts", "string", title: "Google TTS Language", required: true, defaultValue: "en"
	}

	definition (name: "Cast Web API Device", namespace: "zhouguiheng", author: "Vincent Zhou") {
		capability "Speech Synthesis"
		capability "Notification"
        capability "Actuator"
		command "stop"
	}

	tiles(scale: 2) {
		// multi-line text (explicit newlines)
		standardTile("multiLine", "device.multiLine", width: 3, height: 2) {
			state "multiLine", label: "Go to settings to configure the device for your Cast Web API device", defaultState: true
		}
	}
}

def parse(String description) {
	// log.debug "Parsing '${description}'"
}

def speak(message) {
	// log.debug "Executing 'speak'"
    def image = "https://clipartstation.com/wp-content/uploads/2017/11/announcement-clipart-png-8.png"
	def myJson = "[{ \"mediaType\":\"audio/mp3\", \"mediaUrl\":\"\", \"mediaStreamType\":\"BUFFERED\", "
	myJson += "\"mediaTitle\":\"" + message.bytes.encodeBase64() + "\", \"mediaSubtitle\":\"Hubitat notification\", "
	myJson += "\"mediaImageUrl\":\"${image}\", "
	myJson += "\"googleTTS\":\"${googleTts}\"}]"

	def headers = [:]
	headers.put("HOST", "$apiHost:$apiPort")
	headers.put("Content-Type", "application/json")

	def method = "POST"
	def path = "/device/${deviceId}/playMedia"
        // log.debug "${path} ${myJson} ${headers}"

	try {
		def hubAction = new physicalgraph.device.HubAction(
			[
				method: method,
				path: path,
				body: myJson,
				headers: headers
			]
		)

		sendHubCommand(hubAction)
	} catch (Exception e) {
		log.error "Hit Exception $e on $hubAction"
	}
}

def stop() {
	try {
		def headers = [:]
		headers.put("HOST", "$apiHost:$apiPort")
		def hubAction = new physicalgraph.device.HubAction(
			[
				method: "GET",
				path: "/device/${deviceId}/stop",
				headers: headers
			]
		)

		sendHubCommand(hubAction)
	} catch (Exception e) {
		log.error "Hit Exception $e on $hubAction"
	}
}

def deviceNotification(message) {
	speak(message)
}

def installed() {
	sendEvent(name: "multiLine", value: "Click the settings to configure\nthe hostname/IP and port for\nyour Google Assistant Relay")
}