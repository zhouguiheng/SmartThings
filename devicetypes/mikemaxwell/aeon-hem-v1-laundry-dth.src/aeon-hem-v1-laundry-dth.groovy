/*
Custom Laundry monitor device for Aeon HEM V1 

*/

metadata {
	definition (name: "Aeon HEM V1 Laundry DTH", namespace:	"MikeMaxwell", author: "Mike Maxwell") 
	{
		capability "Configuration"
		capability "Switch"
        capability "Button"
        //capability "Energy Meter"
		capability "Actuator"
		capability "Holdable Button"
		capability "Sensor"

        attribute "washerWatts", "string"
        attribute "dryerWatts", "string"
        attribute "washerState", "string"
        attribute "dryerState", "string"
        
//        command "configure"
        
		fingerprint deviceId: "0x2101", inClusters: " 0x70,0x31,0x72,0x86,0x32,0x80,0x85,0x60"
	}

	preferences {
       	input name: "washerRW", type: "number", title: "Washer running watts:", description: "", required: true
        input name: "dryerRW", type: "number", title: "Dryer running watts:", description: "", required: true
    }
	
    simulator {

	}

	tiles(scale: 2) {
    	multiAttributeTile(name:"laundryState", type: "generic", width: 6, height: 4, canChangeIcon: false){
        	tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
//            	attributeState "on", label:'Laundry Running', icon:"st.Appliances.appliances1", backgroundColor:"#53a7c0"
//            	attributeState "off", label:'Laundry Done', icon:"st.Appliances.appliances1", backgroundColor:"#ffffff"
            	attributeState "on", label:'', icon:"st.samsung.da.dryer_ic_dryer", backgroundColor:"#79b821"
            	attributeState "off", label:'', icon:"st.samsung.da.dryer_ic_dryer", backgroundColor:"#ffffff"
        	}
                   
            tileAttribute("device.switch", key: "SECONDARY_CONTROL") {
             	attributeState "on", label:'Laundry Running'
            	attributeState "off", label:'Laundry Done'
    		}
        }   

/*        
        valueTile("washerState", "device.washerState", width: 3, height: 2, canChangeIcon: true) {
        	state "default", label:'Washer\n${currentValue}'        
        }
        valueTile("dryerState", "device.dryerState", width: 3, height: 2, canChangeIcon: true) {
        	state "default", label:'Dryer\n${currentValue}'        
        }
*/
        standardTile("washerState", "device.washerState", width: 3, height: 3, canChangeIcon: true) {
        	state "off", label:'${name}', icon: "st.samsung.da.washer_ic_washer", backgroundColor:"#ffffff"
            state "on", label:'${name}', icon: "st.samsung.da.washer_ic_washer", backgroundColor:"#79b821"
        }
        standardTile("dryerState", "device.dryerState", width: 3, height: 3, canChangeIcon: true) {
        	state "off", label:'${name}', icon: "st.samsung.da.dryer_ic_dryer", backgroundColor:"#ffffff"
            state "on", label:'${name}', icon: "st.samsung.da.dryer_ic_dryer", backgroundColor:"#79b821"
        }

		valueTile("washer", "device.washerWatts", width: 3, height: 2, decoration: "flat") {
            state("default", label:'Washer\n${currentValue} Watts', foregroundColor: "#000000")
        }

		valueTile("dryer", "device.dryerWatts", width: 3, height: 2, decoration: "flat") {
            state("default", label:'Dryer\n${currentValue} Watts', foregroundColor: "#000000")
        }
       
		standardTile("configure", "device.configure", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "configure", label:'', action:"configuration.configure", icon:"st.secondary.configure"
		}

		main "laundryState"
		details(["laundryState","washerState","dryerState","washer","dryer","configure"])
	}
}

def parse(String description) {
	def result = null
	def cmd = zwave.parse(description, [0x31: 1, 0x32: 1, 0x60: 3])
	if (cmd) {
		result = createEvent(zwaveEvent(cmd))
	}
	if (result) { 
		log.debug "Parse returned ${result?.descriptionText}"
		return result
	} else {
	}
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
	//log.info "mc3v cmd: ${cmd}"
	if (cmd.commandClass == 50) {  
    	def encapsulatedCommand = cmd.encapsulatedCommand([0x30: 1, 0x31: 1])
        if (encapsulatedCommand) {
        	def scale = encapsulatedCommand.scale
        	def value = encapsulatedCommand.scaledMeterValue
            def source = cmd.sourceEndPoint
            def str = ""
            def name = ""
        	if (scale == 2 ){ //watts
            	str = "watts"
                if (source == 1){
                	name = "washerWatts"
                    if (value >= settings.washerRW.toInteger()){
                    	//washer is on
                        sendEvent(name: "washerState", value: "on", displayed: true)
                        state.washerIsRunning = true
                    } else {
                    	//washer is off
                        if (state.washerIsRunning == true){
                        	//button event
                            sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "Washer has finished.", isStateChange: true)
                        }
                        sendEvent(name: "washerState", value: "off", displayed: true)
                        state.washerIsRunning = false
                    }
                } else {
                	name = "dryerWatts"
                    if (value >= settings.dryerRW.toInteger()){
                    	//dryer is on
                        sendEvent(name: "dryerState", value: "on", displayed: true)
                        state.dryerIsRunning = true
                    } else {
                    	//dryer is off
                        if (state.dryerIsRunning == true){
                        	//button event
                            sendEvent(name: "button", value: "pushed", data: [buttonNumber: 2], descriptionText: "Dryer has finished.", isStateChange: true)
                        }
                        sendEvent(name: "dryerState", value: "off", displayed: true)
                        state.dryerIsRunning = false
                    }
                }
                if (state.washerIsRunning || state.dryerIsRunning){
                	sendEvent(name: "switch", value: "on", descriptionText: "Laundry has started...", displayed: true)
                } else {
                	sendEvent(name: "switch", value: "off", displayed: false)
                }
                //log.debug "mc3v- name: ${name}, value: ${value}, unit: ${str}"
            	return [name: name, value: value.toInteger(), unit: str, displayed: false]
            }
        }
    }
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
    //log.debug "Unhandled event ${cmd}"
	[:]
}

def configure() {
	log.debug "configure()"
    initialize()
	def cmd = delayBetween([
    	//zwave.configurationV1.configurationSet(parameterNumber: 100, size: 4, scaledConfigurationValue:1).format(),	//reset if not 0
        //zwave.configurationV1.configurationSet(parameterNumber: 110, size: 4, scaledConfigurationValue: 1).format(),	//reset if not 0
        
    	zwave.configurationV1.configurationSet(parameterNumber: 1, size: 2, scaledConfigurationValue: 120).format(),		// assumed voltage
		zwave.configurationV1.configurationSet(parameterNumber: 3, size: 1, scaledConfigurationValue: 0).format(),			// Disable (=0) selective reporting
		zwave.configurationV1.configurationSet(parameterNumber: 9, size: 1, scaledConfigurationValue: 10).format(),			// Or by 10% (L1)
      	zwave.configurationV1.configurationSet(parameterNumber: 10, size: 1, scaledConfigurationValue: 10).format(),		// Or by 10% (L2)
		zwave.configurationV1.configurationSet(parameterNumber: 20, size: 1, scaledConfigurationValue: 1).format(),			//usb = 1
		zwave.configurationV1.configurationSet(parameterNumber: 101, size: 4, scaledConfigurationValue: 6912).format(),   	
		zwave.configurationV1.configurationSet(parameterNumber: 111, size: 4, scaledConfigurationValue: 30).format() 		// Every 30 seconds
	], 2000)

	return cmd
}

def installed() {
	configure()
}

def updated() {
	configure()
}

def initialize() {
	sendEvent(name: "numberOfButtons", value: 2)
}