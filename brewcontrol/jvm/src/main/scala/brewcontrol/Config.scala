package brewcontrol

class Config()(implicit db: DB) {

  val targetTemperature = db.Props.asVarSync("targetTemperature", 20d)

  val heaterEnabled = db.Props.asVarSync("heaterEnabled", false)
  val coolerEnabled = db.Props.asVarSync("coolerEnabled", false)
}
