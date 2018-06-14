import board
import digitalio
#from digitalio import DigitalInOut, Direction
import time
import busio
import adafruit_bno055

# Bluetooth
uart = busio.UART(board.TX, board.RX, baudrate=9600)

# Gyro
i2c = busio.I2C(board.SCL, board.SDA)
sensor = adafruit_bno055.BNO055(i2c)

# LED
led = digitalio.DigitalInOut(board.D13)
led.direction = digitalio.Direction.OUTPUT

# Button
button = digitalio.DigitalInOut(board.D6)
button.direction = digitalio.Direction.INPUT

for i in range(3):
    led.value = True
    time.sleep(0.1)
    led.value = False
    time.sleep(0.1) 
print("Setup done!\n")

# Main loop runs forever printing acceleration and Euler angles every second.
while True:
    if button.value:
        txString = "bp+"+str(sensor.euler)+"+"+str(sensor.quaternion)+"+"+str(sensor.gyroscope)+"@"
        #print('Accelerometer (m/s^2): {}'.format(sensor.accelerometer))
        #print('Euler angle: {}'.format(sensor.euler))
        #print('Quaterion: {}'.format(sensor.quaternion))
        #print('Qyroscope: {}'.format(sensor.gyroscope))
        #print("- "*20)
        #print(txString)
        #x = input("Message to send\n")
        print(uart.write(txString))
        time.sleep(0.1)
        
        
    txString = "sp+"+str(sensor.euler)+"+"+str(sensor.quaternion)+"+"+str(sensor.gyroscope)+"@"    
    uart.write(txString)    
    
    #data = None
    data = uart.read(2)  # read up to 32 bytes
    if data:
        datastr = ''.join([chr(b) for b in data])# convert bytearray to string
        if datastr == "r@":
            txString = "bp+"+str(sensor.euler)+"+"+str(sensor.quaternion)+"+"+str(sensor.gyroscope)+"@"    
            uart.write(txString)
        else:
            print(datastr)
            