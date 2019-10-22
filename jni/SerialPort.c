/*
 * Copyright 2009 Cedric Priscal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <errno.h>

#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <jni.h>

#include "android/log.h"
static const char *TAG="serial_port";
#define LOGI(fmt, args...) __android_log_print(ANDROID_LOG_INFO,  TAG, fmt, ##args)
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, TAG, fmt, ##args)
#define LOGE(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, TAG, fmt, ##args)

static int fd_port = -1;

static speed_t getBaudrate(jint baudrate)
{
	switch(baudrate) {
	case 0: return B0;
	case 50: return B50;
	case 75: return B75;
	case 110: return B110;
	case 134: return B134;
	case 150: return B150;
	case 200: return B200;
	case 300: return B300;
	case 600: return B600;
	case 1200: return B1200;
	case 1800: return B1800;
	case 2400: return B2400;
	case 4800: return B4800;
	case 9600: return B9600;
	case 19200: return B19200;
	case 38400: return B38400;
	case 57600: return B57600;
	case 115200: return B115200;
	case 230400: return B230400;
	case 460800: return B460800;
	case 500000: return B500000;
	case 576000: return B576000;
	case 921600: return B921600;
	case 1000000: return B1000000;
	case 1152000: return B1152000;
	case 1500000: return B1500000;
	case 2000000: return B2000000;
	case 2500000: return B2500000;
	case 3000000: return B3000000;
	case 3500000: return B3500000;
	case 4000000: return B4000000;
	default: return -1;
	}
}

/* baudrate settings are defined in <asm/termbits.h>, which is
included by <termios.h> */
#define BAUDRATE B115200        
/* change this definition for the correct port */
#define MODEMDEVICE "/dev/ttyS1"
#define _POSIX_SOURCE 1 /* POSIX compliant source */

 int init_tty()
{
  int fd,c, res;
  struct termios oldtio,newtio;
//  char buf[255];
/* 
  Open modem device for reading and writing and not as controlling tty
  because we don't want to get killed if linenoise sends CTRL-C.
*/
 fd = open(MODEMDEVICE, O_RDWR | O_NOCTTY |  O_SYNC);
 if (fd <0) {perror(MODEMDEVICE); return fd; }

 tcgetattr(fd,&oldtio); /* save current serial port settings */
 //bzero(&newtio, sizeof(newtio)); /* clear struct for new port settings */
 memset(&newtio, 0, sizeof(newtio));  

/* 
  BAUDRATE: Set bps rate. You could also use cfsetispeed and cfsetospeed.
  CRTSCTS : output hardware flow control (only used if the cable has
            all necessary lines. See sect. 7 of Serial-HOWTO)
  CS8     : 8n1 (8bit,no parity,1 stopbit)
  CLOCAL  : local connection, no modem contol
  CREAD   : enable receiving characters
*/
 newtio.c_cflag = BAUDRATE | CRTSCTS | CS8 | CLOCAL | CREAD;
 
/*
  IGNPAR  : ignore bytes with parity errors
  ICRNL   : map CR to NL (otherwise a CR input on the other computer
            will not terminate input)
  otherwise make device raw (no other input processing)
*/
 newtio.c_iflag = IGNPAR | ICRNL;
 
/*
 Raw output.
*/
 newtio.c_oflag = 0;
 
/*
  ICANON  : enable canonical input
  disable all echo functionality, and don't send signals to calling program
*/
 newtio.c_lflag = ICANON;
 
/* 
  initialize all control characters 
  default values can be found in /usr/include/termios.h, and are given
  in the comments, but we don't need them here
*/
 newtio.c_cc[VINTR]    = 0;     /* Ctrl-c */ 
 newtio.c_cc[VQUIT]    = 0;     /* Ctrl-\ */
 newtio.c_cc[VERASE]   = 0;     /* del */
 newtio.c_cc[VKILL]    = 0;     /* @ */
 newtio.c_cc[VEOF]     = 4;     /* Ctrl-d */
 newtio.c_cc[VTIME]    = 0;     /* inter-character timer unused */
 newtio.c_cc[VMIN]     = 1;     /* blocking read until 1 character arrives */
 newtio.c_cc[VSWTC]    = 0;     /* '\0' */
 newtio.c_cc[VSTART]   = 0;     /* Ctrl-q */ 
 newtio.c_cc[VSTOP]    = 0;     /* Ctrl-s */
 newtio.c_cc[VSUSP]    = 0;     /* Ctrl-z */
 newtio.c_cc[VEOL]     = 0;     /* '\0' */
 newtio.c_cc[VREPRINT] = 0;     /* Ctrl-r */
 newtio.c_cc[VDISCARD] = 0;     /* Ctrl-u */
 newtio.c_cc[VWERASE]  = 0;     /* Ctrl-w */
 newtio.c_cc[VLNEXT]   = 0;     /* Ctrl-v */
 newtio.c_cc[VEOL2]    = 0;     /* '\0' */

/* 
  now clean the modem line and activate the settings for the port
*/
 tcflush(fd, TCIFLUSH);
 tcsetattr(fd,TCSANOW,&newtio);

 return fd;
#if 0
/*
  terminal settings done, now handle input
  In this example, inputting a 'z' at the beginning of a line will 
  exit the program.
*/
 while (STOP==FALSE) {     /* loop until we have a terminating condition */
 /* read blocks program execution until a line terminating character is 
    input, even if more than 255 chars are input. If the number
    of characters read is smaller than the number of chars available,
    subsequent reads will return the remaining chars. res will be set
    to the actual number of characters actually read */
    res = read(fd,buf,255); 
    buf[res]=0;             /* set end of string, so we can printf */
    printf(":%s:%d\n", buf, res);
    if (buf[0]=='z') STOP=TRUE;
 }
 /* restore the old port settings */
 tcsetattr(fd,TCSANOW,&oldtio);
 #endif
}

/*
 * Class:     cedric_serial_SerialPort
 * Method:    open
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT jobject JNICALL Java_android_serialport_SerialPort_open
  (JNIEnv *env, jobject thiz, jstring path, jint baudrate)
{
	int fd;
	speed_t speed;
	jobject mFileDescriptor;
#if 1
	/* Check arguments */
	{
		speed = getBaudrate(baudrate);
		if (speed == -1) {
			/* TODO: throw an exception */
			LOGE("Invalid baudrate");
			return NULL;
		}
	}
#endif

	/* Opening device */
	{
		jboolean iscopy;
		const char *path_utf = (*env)->GetStringUTFChars(env, path, &iscopy);
		LOGD("Opening serial port %s", path_utf);
		//fd = open(path_utf, O_RDWR | O_DIRECT | O_SYNC);
		  fd = open(path_utf, O_RDWR | O_NOCTTY |  O_SYNC);
		//fd = init_tty();
		LOGD("open() fd = %d", fd);
		LOGD("open() errno = %d", errno);
		(*env)->ReleaseStringUTFChars(env, path, path_utf);
		if (fd == -1)
		{
			/* Throw an exception */
			LOGE("Cannot open port");
			/* TODO: throw an exception */
			return NULL;
		}
	}
#if 1
	/* Configure device */
	{
		struct termios cfg;
		LOGD("Configuring serial port");
		if (tcgetattr(fd, &cfg))
		{
			LOGE("tcgetattr() failed");
			close(fd);
			/* TODO: throw an exception */
			return NULL;
		}

		cfmakeraw(&cfg);
		cfsetispeed(&cfg, speed);
		cfsetospeed(&cfg, speed);

		if (tcsetattr(fd, TCSANOW, &cfg))
		{
			LOGE("tcsetattr() failed");
			close(fd);
			/* TODO: throw an exception */
			return NULL;
		}
	}
#endif
	/* Create a corresponding file descriptor */
	{
		jclass cFileDescriptor = (*env)->FindClass(env, "java/io/FileDescriptor");
		jmethodID iFileDescriptor = (*env)->GetMethodID(env, cFileDescriptor, "<init>", "()V");
		jfieldID descriptorID = (*env)->GetFieldID(env, cFileDescriptor, "descriptor", "I");
		mFileDescriptor = (*env)->NewObject(env, cFileDescriptor, iFileDescriptor);
		(*env)->SetIntField(env, mFileDescriptor, descriptorID, (jint)fd);
	}

	fd_port = fd;
	return mFileDescriptor;
}

JNIEXPORT jint JNICALL Java_android_serialport_SerialPort_write
  (JNIEnv *env, jobject thiz, jint fd, jbyteArray buffer, jint len)
{
	jint ret = -1;
	jbyte* bufferPtr = (*env)->GetByteArrayElements(env, buffer, NULL);
	if (bufferPtr == NULL) {
		LOGE("GetByteArrayElements() failed");
		return -1;
	}

	ret = write(fd, bufferPtr, len);
	(*env)->ReleaseByteArrayElements(env, buffer, bufferPtr, 0);

	return ret;
}

#define TEST_BUFFER_SIZE 128
/*
 * Class:     android_serialport_SerialPort
 * Method:    readArray
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_android_serialport_SerialPort_readArray
  (JNIEnv *env, jobject thiz, jint fd)
{

		//传递JNI层的数组数据到Java端，有两种方法，一种是本例所示的通过返回值来传递
		//另一种是通过回调Java端的函数来传递(多用于jni线程中回调java层)
		unsigned char buffer[TEST_BUFFER_SIZE];

		int res=0;

		res = read(fd_port,buffer,TEST_BUFFER_SIZE);

		//分配ByteArray
		jbyteArray array = NULL;

		if (res > 0) {
			array = (*env)->NewByteArray(env,res);

			//将传递数据拷贝到java端
			(*env)->SetByteArrayRegion(env, array, 0, res, buffer);
		}

		return array;
}

/*
 * Class:     cedric_serial_SerialPort
 * Method:    close
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_android_serialport_SerialPort_close
  (JNIEnv *env, jobject thiz)
{
	jclass SerialPortClass = (*env)->GetObjectClass(env, thiz);
	jclass FileDescriptorClass = (*env)->FindClass(env, "java/io/FileDescriptor");

	jfieldID mFdID = (*env)->GetFieldID(env, SerialPortClass, "mFd", "Ljava/io/FileDescriptor;");
	jfieldID descriptorID = (*env)->GetFieldID(env, FileDescriptorClass, "descriptor", "I");

	jobject mFd = (*env)->GetObjectField(env, thiz, mFdID);
	jint descriptor = (*env)->GetIntField(env, mFd, descriptorID);

	LOGD("close(fd = %d)", descriptor);
	close(descriptor);
}


JNIEXPORT jint JNICALL Java_android_serialport_SerialPort_readGPIO
  (JNIEnv *env, jobject thiz, jint gpio)
{
        int fd_export, fd_dir, fd_val, fd_unexport;
        char buf[128];
        char value;
        /*      int gpio;

      if ((argc!=3)&&(argc!=4))
        {
            printf("GPIO Read/Write test app\n");
            printf("gpio_num [direction 1:output 0:input] [output 1:high/0:low]\n");
            printf("Ex: 38 1 1\n");
            return 0;
        }*/


        //gpio = atoi(argv[1]);
        LOGD("gpio = %d \n", gpio);
        fd_export = open("/sys/class/gpio/export", O_WRONLY);
        LOGD("open() errno = %d", errno);
        fd_unexport = open("/sys/class/gpio/unexport", O_WRONLY);
        LOGD("open() errno = %d", errno);

        sprintf(buf, "/sys/class/gpio/gpio%d", gpio);
        if(access(buf, F_OK))
        {
            sprintf(buf, "%d", gpio);
            write(fd_export, buf, strlen(buf));
        }

        sprintf(buf, "/sys/class/gpio/gpio%d/direction", gpio);
        fd_dir = open(buf, O_WRONLY);
        LOGD("open() errno = %d", errno);
        sprintf(buf, "/sys/class/gpio/gpio%d/value", gpio);
        fd_val = open(buf, O_RDWR);
        LOGD("open() errno = %d", errno);

/*        if(atoi(argv[2]))
        {
            write(fd_dir, "out", 3); // Set out direction
            if(atoi(argv[3]))
            {
                write(fd_val, "1", 1); // Set GPIO high status
                printf("gpio output high\n");
            }else{
                write(fd_val, "0", 1); // Set GPIO low status
                printf("gpio output low\n");
            }
        }else*/
        {
            write(fd_dir, "in", 2); // Set in direction
            read(fd_val, &value, 1);
            if(value == '0')
            	LOGD("gpio%d input low \n", gpio); // Current GPIO status low
            else
            	LOGD("gpio%d input high \n", gpio); // Current GPIO status high
        }

        //sprintf(buf, "%d", gpio);
        //write(fd_unexport, buf, strlen(buf));

        close(fd_export);
        close(fd_dir);
        close(fd_val);
        close(fd_unexport);

        return value == '0' ? 0 : 1 ;
}

