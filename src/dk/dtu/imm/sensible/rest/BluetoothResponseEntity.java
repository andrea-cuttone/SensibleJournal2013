package dk.dtu.imm.sensible.rest;

/*
{
	timestamp: 1338121167,
	uid: "351565053474810",
	devices: 
		[
		{
			name: "my MacBook Pro",
			mac_address: "5C:59:48:CB:D1:A1"
			sensible_user_id: "457840db734ddef73749fce6ed72c5fb"
		},
		{
			name: "john doe",
			mac_address: "00:1F:3A:E5:02:21"
		}
		]
}

 */
public class BluetoothResponseEntity {
	public long timestamp;
	public Device [] devices;
	
	public static class Device {
		public String sensible_user_id;
	}
	
	public BluetoothResponseEntity(long timestamp, String uid) {
		this.timestamp = timestamp;
		devices = new Device[1];
		devices[0] = new Device();
		devices[0].sensible_user_id = uid;
	}

}
