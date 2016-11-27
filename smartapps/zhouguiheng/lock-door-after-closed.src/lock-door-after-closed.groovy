/**
 *  Lock Door after Closed with Retries
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
    name: "Lock Door after Closed",
    namespace: "zhouguiheng",
    author: "Vincent",
    description: "Automatically lock the door after it's closed. Specially designed for Schlage Connect",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Select the door lock:") {
		input "thelock", "capability.lock", required: true
	}
    section("Select the door contact sensor:") {
    	input "thesensor", "capability.contactSensor", required: true
    }
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(thesensor, "contact.closed", closeHandler)
	subscribe(thelock, "lock.unlocked", unlockHandler)
}

def closeHandler(evt) {
	lockTheDoor()
}

def unlockHandler(evt) {
	// It requires at least 10 seconds for Schlage Connect to accept lock command after unlocked.
	runIn(11, lockTheDoor, [overwrite: false])
    runIn(15, lockTheDoor, [overwrite: false])
    runIn(15 * 60, checkDoorLocked)
}

def lockTheDoor() {
    if ((thesensor.latestValue("contact") == "closed") && (thelock.latestValue("lock") != "locked")) {
		thelock.lock()
    }
}

def checkDoorLocked() {
	if (thelock.latestValue("lock") != "locked") {
    	lockTheDoor()
    	sendPush("Door is not locked for 15 minutes!")
    }
}