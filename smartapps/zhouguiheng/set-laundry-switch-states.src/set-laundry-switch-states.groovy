/**
 *  Set laundry switch states according to power meters.
 *
 *  Copyright 2017 Vincent
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
    name: "Set Laundry Switch States",
    namespace: "zhouguiheng",
    author: "Vincent",
    description: "Set the states of laundry switches according to power meters.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Washer:") {
		input "washer", "capability.switch", required: true
	}
	section("Dryer:") {
		input "dryer", "capability.switch", required: true
	}
    section("Power meter:") {
    	input "power", "capability.powerMeter", required: true
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
	subscribe(power, "powerOne", washerHandler)
	subscribe(power, "powerTwo", dryerHandler)
    state.tryingWasherOn = false
    state.tryingWasherOff = false
}

def washerHandler(evt) {
  try {
    def p = evt.doubleValue
    log.debug "Washer power: ${p}"
    if (p > 12) {
      state.tryingWasherOff = false
      unschedule(washerOff)
      if (p > 20) {
        washerOn()
      } else {
        if (washer.currentSwitch != "on" && !state.tryingWasherOn) {
          log.debug "Trying washer on"
          state.tryingWasherOn = true
          runIn(120, washerOn)
        }
      }
    } else {
      state.tryingWasherOn = false
      unschedule(washerOn)
      if (washer.currentSwitch == "on" && !state.tryingWasherOff) {
        log.debug "Trying washer off"
        state.tryingWasherOff = true
        runIn(60, washerOff)
      }
    }
  } catch (e) {
    log.debug("Failed to get double value for ${evt.name}", e)
  }
}

def washerOn() {
  state.tryingWasherOn = false
  if (washer.currentSwitch != "on") {
    log.debug "Turn on washer"
    washer.on()
  }
}

def washerOff() {
  state.tryingWasherOff = false
  if (washer.currentSwitch != "off") {
    log.debug "Turn off washer"
    washer.off()
  }
}

def dryerHandler(evt) {
  try {
    def p = evt.doubleValue
    log.debug "Dryer power: ${p}"
    if (p > 20) {
      if (dryer.currentSwitch != "on") {
        log.debug "Turn on dryer"
        dryer.on()
      }
    } else {
      if (dryer.currentSwitch != "off") {
        log.debug "Turn off dryer"
        dryer.off()
      }
    }
  } catch (e) {
    log.debug("Failed to get double value for ${evt.name}", e)
  }
}