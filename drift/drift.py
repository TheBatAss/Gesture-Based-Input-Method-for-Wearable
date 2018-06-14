# Search for BLE UART devices and list all that are found.
# Author: Tony DiCola
import atexit
import time
import thread
from datetime import datetime
import os.path

import Adafruit_BluefruitLE
from Adafruit_BluefruitLE.services import UART

secondsBetweenSamples = 60

btID = "1e371702-6e16-426f-a8fa-3c6f9337eeca"
run4ever = True
uart = None

# Get the BLE provider for the current platform.
ble = Adafruit_BluefruitLE.get_provider()


# Main function implements the program logic so it can run in a background
# thread.  Most platforms require the main thread to handle GUI events and other
# asyncronous events like BLE actions.  All of the threading logic is taken care
# of automatically though and you just need to provide a main function that uses
# the BLE provider.
def main():
    global run4ever
    global uart
    print('run4ever: '+str(run4ever))
    print2Log('run4ever: '+str(run4ever))
    
    # Clear any cached data because both bluez and CoreBluetooth have issues with
    # caching data and it going stale.
    ble.clear_cached_data()

    # Get the first available BLE network adapter and make sure it's powered on.
    adapter = ble.get_default_adapter()
    adapter.power_on()
    print('Using adapter: {0}'.format(adapter.name))
    print2Log('Using adapter: {0}'.format(adapter.name))

    # Start scanning with the bluetooth adapter.
    adapter.start_scan()
    # Use atexit.register to call the adapter stop_scan function before quiting.
    # This is good practice for calling cleanup code in this main function as
    # a try/finally block might not be called since this is a background thread.
    atexit.register(adapter.stop_scan)
    print('Searching for UART devices...')
    print('Press Ctrl-C to quit (will take ~30 seconds on OSX).')
    print2Log('Searching for UART devices...')
    print2Log('Press Ctrl-C to quit (will take ~30 seconds on OSX).')
    # Enter a loop and print out whenever a new UART device is found.
    known_uarts = set()
    device = 0
    c = True
    while c:
        # Call UART.find_devices to get a list of any UART devices that
        # have been found.  This call will quickly return results and does
        # not wait for devices to appear.
        found = set(UART.find_devices())
        # Check for new devices that haven't been seen yet and print out
        # their name and ID (MAC address on Linux, GUID on OSX).
        new = found - known_uarts
        for dev in new:
            print('Found UART: {0} [{1}]'.format(dev.name, dev.id))
            print2Log('Found UART: {0} [{1}]'.format(dev.name, dev.id))
            if str(dev.id) == btID:
                #print('Device found')
                print2Log('Device found')
                device = dev
                adapter.stop_scan()
                c = False
                
        known_uarts.update(new)
        # Sleep for a second and see if new devices have appeared.
        time.sleep(1.0)
    
    print('Connecting to device...')
    print2Log('Connecting to device...')
    device.connect()  # Will time out after 60 seconds, specify timeout_sec parameter
                      # to change the timeout.

    # Once connected do everything else in a try/finally to make sure the device
    # is disconnected when done.
    try:
        # Wait for service discovery to complete for the UART service.  Will
        # time out after 60 seconds (specify timeout_sec parameter to override).
        print('Discovering services...')
        print2Log('Discovering services...')
        UART.discover(device)

        # Once service discovery is complete create an instance of the service
        # and start interacting with it.
        uart = UART(device)

        # Write a string to the TX characteristic.
        #uart.write('Hello world!\r\n')
        #print("Sent 'Hello world!' to the device.")

        # Now wait up to one minute to receive data from the device.
        #print('Waiting up to 60 seconds to receive data from the device...')
        
        print("Comm loop runing")
        print2Log("Comm loop runing")
        stopChar = '@'
        messageBuffer = ""
        while run4ever:
            
            received = uart.read(timeout_sec=0.05)
            if received:
                # Received data, print it out.
                print('Received:{0}'.format(received))
                print2Log('Received:{0}'.format(received))
            #else:
                # Timeout waiting for data, None is returned.
                print('Received no data!')
                messageBuffer += received
                while stopChar in messageBuffer:
                    cut = messageBuffer.split(stopChar)
                    finalMessage = cut[0]
                    #print("Final Mesage:"+finalMessage)
                    #print2Log("Final Mesage:"+finalMessage)
                    processInput(finalMessage)
                    messageBuffer = messageBuffer[len(cut[0])+1:]

    
    except Exception as e:
        print(e)
    
    finally:
        # Make sure device is disconnected on exit.
        while run4ever:
            pass
        device.disconnect()


# Initialize the BLE system.  MUST be called before other BLE calls!
ble.initialize()

# Start the mainloop to process BLE events, and run the provided function in
# a background thread.  When the provided main function stops running, returns
# an integer status code, or throws an error the program will exit.
def runBLE():
    ble.run_mainloop_with(main)
    
def transmitString(s):
    global uart
    uart.write(s)
    
    
# # # # # # # # # # # # # # # # # # # # # # # # # # # #
# GUI # GUI # GUI # GUI # GUI # GUI # GUI # GUI # GUI # 
# # # # # # # # # # # # # # # # # # # # # # # # # # # #
from Tkinter import *
import ScrolledText as tkst
import tkFont

root = Tk()
root.wm_title('Data Collector!')

def quit():
    global run4ever
    root.destroy()
    run4ever = False
    time.sleep(0.1)
    print("Quiting")
    
def print2Log(txt,log=0):
    global rawLog
    global requestData
    global spamData
    logs = [rawLog, requestData, spamData]
    logs[log].config(state=NORMAL)
    logs[log].insert('end', txt+'\n')
    logs[log].see('end')
    #logs[log].config(state=DISABLED)

def processInput(txt):
    global dataCount
    if txt[:2] == 'bp':
        cut = txt.split('+')
        calCut = cut[2].split(',')
        euler = 'Euler angle: {}\n'.format(cut[1])
        cali = 'Calibration: s'+calCut[0]+' g'+calCut[1]+' a'+calCut[2]+' m'+calCut[3]+'\n'
        print2Log(euler+cali+"- "*20,1)
        
        # Save to file
        tn = str(datetime.now())[11:-7]
        dataCount += 1
        with open("output.csv",'a') as f:
            f.write((str(dataCount)+","+tn+","+cut[1]+","+cut[2]+"\n").translate(None,"()"))
        print2Log("data collected: "+str(dataCount),2)
        
        
        
    elif txt[:2] == 'sp':
        cut = txt.split('+')
        calCut = cut[2].split(',')
        euler = 'Euler angle: {}\n'.format(cut[1])
        cali = 'Calibration: s'+calCut[0]+' g'+calCut[1]+' a'+calCut[2]+' m'+calCut[3]+'\n'
        print2Log(euler+cali+"- "*20,2)
    else:
        print(txt)
        print2Log(txt)
        
logData = False
dataCount = 0
    
        
def waitNcall():
    global secondsBetweenSamples
    while logData:
        transmitString("r@")
        time.sleep(secondsBetweenSamples)
                
def startDataCollection():
    global logData
    global dataBt
    global dataCount
    logData = not logData
    
    if logData:
        dataBt['text'] = "Stop"
        dataCount = 0
        print2Log("Data collection started",2)
        header = ["id", "time",
                "Euler1", "Euler2", "Euler3",
                "SysCal", "GyroCal", "AccCal", "MagCal"]
            
        with open("output.csv","a") as f:
            f.write(",".join(header)+"\n")
        try:
            thread.start_new_thread(waitNcall,())
            
        except:
            raise
    else:
        dataBt['text'] = "Start"
        print2Log("Data collection stopped",2)
    

bgColor = 'white smoke'

topRowFrame = Frame(root)
clickFrame = Frame(topRowFrame)
lefFrame = Frame(topRowFrame)
rightFrame = Frame(topRowFrame)
bottomRowFrame = Frame(root)

quitBt = Button(clickFrame, text="QUIT", command= lambda: quit())
quitBt.pack()
whiteSpace = Label(clickFrame, text="\n"*10)
whiteSpace.pack()
dataBt = Button(clickFrame, text="Start", command= lambda: startDataCollection())
dataBt.pack()

requestDataTitle = Label(lefFrame, text="Requested data:")
requestData = tkst.ScrolledText(master=lefFrame,
    wrap   = 'word',  # wrap text at full words only
    width  = 60,      # characters
    height = 20,      # text lines
    bg=bgColor        # background color of edit area
    )
requestData.config(state=DISABLED)
requestDataTitle.grid(sticky='w')
requestData.grid()
lefFrame.grid(row=0, column=0)

spamDataTitle = Label(rightFrame, text="Spammed data:")
spamData = tkst.ScrolledText(master=rightFrame,
    wrap   = 'word',  # wrap text at full words only
    width  = 60,      # characters
    height = 20,      # text lines
    bg=bgColor        # background color of edit area
    )
spamData.config(state=DISABLED)
spamDataTitle.grid(sticky='w')
spamData.grid()
rightFrame.grid(row=0, column=3)

clickFrame.columnconfigure(0, weight=1)
clickFrame.grid(row=0, column=1)

topRowFrame.pack()
bottomRowFrame.pack()

ralLogTitle = Label(bottomRowFrame, text="Debugging log:")
ralLogTitle.grid(sticky='w')
rawLog = tkst.ScrolledText(master=bottomRowFrame,
    wrap   = 'word',  # wrap text at full words only
    width  = 135,      # characters
    height = 20,      # text lines
    bg=bgColor        # background color of edit area
    )

rawLog.grid()


# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
# Start BLE and GUI # Start BLE and GUI # Start BLE and GUI #
# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
try:
    thread.start_new_thread(runBLE,())
except:
    raise

root.mainloop()