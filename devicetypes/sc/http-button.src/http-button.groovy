/*
*  HTTP Button
*  Category: Device Handler
* 
*  Source: https://community.smartthings.com/t/beta-release-uri-switch-device-handler-for-controlling-items-via-http-calls/37842
* 
*  Credit: tguerena, surge919, CSC
*  v1.1 - changes to enhance the behavior of the button and make it a momentary button
*/

import groovy.json.JsonSlurper

metadata {
	definition (name: "HTTP Button", namespace: "sc", author: "SC") {
	capability "Actuator"
    capability "Switch"
	capability "Sensor"
	}

	preferences {
		input("DeviceIP", "string", title:"Device IP Address", description: "Please enter your device's IP Address", required: true, displayDuringSetup: true)
		input("DevicePort", "string", title:"Device Port", description: "Empty assumes port 80.", required: false, displayDuringSetup: true)
		input("DevicePathOn", "string", title:"URL Path for ON", description: "Rest of the URL, include forward slash.", displayDuringSetup: true)
		input("DevicePathOff", "string", title:"URL Path for OFF", description: "Rest of the URL, include forward slash.", displayDuringSetup: true)
		input(name: "DevicePostGet", type: "enum", title: "POST or GET", options: ["POST","GET"], defaultValue: "POST", required: false, displayDuringSetup: true)
		section() {
			input("HTTPAuth", "bool", title:"Requires User Auth?", description: "Choose if the HTTP requires basic authentication", defaultValue: false, required: true, displayDuringSetup: true)
			input("HTTPUser", "string", title:"HTTP User", description: "Enter your basic username", required: false, displayDuringSetup: true)
			input("HTTPPassword", "string", title:"HTTP Password", description: "Enter your basic password", required: false, displayDuringSetup: true)
		}
	}


	// simulator metadata
	simulator {
	}

	// UI tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
			state "off", label: 'OFF', action: "on", backgroundColor: "#ffffff", nextState: "on"
			state "on", label: 'ON', action: "off", backgroundColor: "#53a7c0", nextState: "off"
		}
		main "switch"
		details "switch"
	}
}

def parse(String description) {
	log.debug(description)
}

def on() {
	log.debug "---Sending command--- ${DevicePathOn}"
	sendEvent(name: "switch", value: "on", isStateChange: true, display: false)
	runCmd(DevicePathOn)
}

def off() {
	sendEvent(name: "switch", value: "off", isStateChange: true, display: false)
	runCmd(DevicePathOff)
}

def runCmd(String varCommand) {
	def host = DeviceIP
	def LocalDevicePort = ''
	if (DevicePort==null) { LocalDevicePort = "80" } else { LocalDevicePort = DevicePort }

	def userpassascii = "${HTTPUser}:${HTTPPassword}"
	def userpass = "Basic " + userpassascii.encodeAsBase64().toString()

	log.debug "The device id configured is: $device.deviceNetworkId"

	def path = varCommand
	log.debug "path is: $path"
	//log.debug "Uses which method: $DevicePostGet"
	def body = "" 
	//log.debug "body is: $body"

	def headers = [:] 
	headers.put("HOST", "$host:$LocalDevicePort")
	headers.put("Content-Type", "application/x-www-form-urlencoded")
	if (HTTPAuth) {
		headers.put("Authorization", userpass)
	}
	log.debug "The Header is $headers"
	def method = "POST"
	try {
		if (DevicePostGet.toUpperCase() == "GET") {
			method = "GET"
			}
		}
	catch (Exception e) {
		settings.DevicePostGet = "POST"
		log.debug e
		log.debug "You must not have set the preference for the DevicePOSTGET option"
	}
	log.debug "The method is $method"
	try {
		def hubAction = new physicalgraph.device.HubAction(
			method: method,
			path: path,
			body: body,
			headers: headers
			)
		log.debug hubAction
		return hubAction
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on $hubAction"
	}
}