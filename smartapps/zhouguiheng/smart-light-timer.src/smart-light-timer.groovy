/**
 *  Smart Light Timer for Vincent
 *
 *  Copyright 2016 Vincent
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
definition(
    name: "Smart Light Timer",
    namespace: "zhouguiheng",
    author: "Vincent",
    description: "Turn on/off the lights based on sensors.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Turn on/off light..."){
		input "myswitch", "capability.switch", title: "Select Light"
	}
	section("Turn on when...") {
		input "motions", "capability.motionSensor", multiple: true, title: "Motion detected", required: false
		input "contacts", "capability.contactSensor", multiple: true, title: "Contacts open", required: false
	}
	section("Turn off after...") {
        input "motionMinutes", "number", title: "Minutes to turn off after motion stops", defaultValue: "1"
        input "minutes1", "number", title: "Minutes to turn off after no other triggers", defaultValue: "10"
    }
    section("The switch to prevent the light from being turned off") {
		input "holder", "capability.switch", title: "Select Light Holder", required: false
    }
    section("Enable debug logging") {
        input "debuglog", "bool", title: "Enabled?", defaultValue: "false"
    }
}

def installed()
{
	subscribe(myswitch, "switch", lightHandler)
	subscribe(motions, "motion", motionHandler)
	subscribe(contacts, "contact", contactHandler)
	if (holder != null) subscribe(holder, "switch", holderHandler)
}

def updated()
{
	unsubscribe()
    installed()
}

def lightHandler(evt) {
	if (debuglog) log.debug "lightHandler from $evt.source: $evt.name: $evt.value"
    if (evt.value == "on") {
  		scheduleTurnOff(minutes1)
    } else if (evt.value == "off") {
    	unschedule(turnOff)
    }
}

def contactHandler(evt) {
	if (debuglog) log.debug "contactHandler: $evt.name: $evt.value"
    if (evt.value == "open") {
    	turnOn(minutes1)
    }
}

def motionHandler(evt) {
	if (debuglog) log.debug "motionHandler: $evt.name: $evt.value"
    if (evt.value == "active") {
    	turnOn(minutes1)
    } else if (evt.value == "inactive") {
    	scheduleTurnOff(motionMinutes) 
    }
}

def holderHandler(evt) {
	if (debuglog) log.debug "holderHandler: $evt.name: $evt.value, $holderOffTime"
    if (evt.value == "on") {
    	turnOn(-1)
        unschedule(turnOff)
    } else {
    	turnOff()
    }
}

def turnOn(minutes) {
	if (debuglog) log.debug "turnOn: " + myswitch.latestValue("switch")
	if (myswitch.latestValue("switch") == "off") {
		myswitch.on()
    }
    if (minutes > 0) scheduleTurnOff(minutes)
}

def scheduleTurnOff(minutes) {
	if (debuglog) log.debug "scheduleTurnOff: $minutes minutes"
    if (holder == null || holder.latestValue("switch") != "on") {
        if (minutes > 0) {
          runIn(minutes * 60, turnOff)
        } else {
          unschedule(turnOff)
          turnOff()
        }
    }
}

def turnOff() {
	if (debuglog) log.debug "turnOff: " + myswitch.latestValue("switch")
	if (myswitch.latestValue("switch") == "on") {
    	if (motions.find { it.latestValue("motion") == "active" } == null) {
			myswitch.off()
        } else {
        	// Re-check after 1 minute.
        	scheduleTurnOff(1)
        }
    }
}