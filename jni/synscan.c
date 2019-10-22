/* gcc -o synscan synscan.c -lpthread
mipsel-linux-gcc  -o synscan synscan.c -lpthread --static; mipsel-linux-strip synscan
*/
#include <netdb.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <stdio.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <net/if.h>
#include <signal.h>
#include <netinet/ip.h>
#include <netinet/in.h>
#include <string.h>
#include <arpa/inet.h>
#include <netinet/tcp.h>
#include <pthread.h>
#include <signal.h>

#define SEQ 20020
#define TCPSIZE sizeof(struct tcphdr)
int sendSyn(int sockfd,u_long sourceIP,u_short sourcePort,u_long seqNum,struct sockaddr_in* dst);
void* recv_packet(void* arg);


int64_t TimerGetTickCount()
{
    struct timeval t;
    gettimeofday(&t, 0);
    return (((int64_t)t.tv_sec * 1000LL) + (int64_t)(t.tv_usec / 25000) * 25);
}

unsigned short in_cksum(unsigned short* addr,int len) //checksum
{
    int nleft=len;
    int sum=0;
    unsigned short* w=addr;
    unsigned short answer=0;
    while (nleft> 1)
    {
        sum+=*w++;
        nleft-=2;
    }
    if (nleft==1)
    {
        *(unsigned char*)(&answer)=*(unsigned char*)w;
        sum+=answer;
    }
    sum=(sum >> 16)+(sum & 0xffff);
    sum+=(sum >> 16);
    answer=~sum;
    return(answer);
}

void Alarm(int sig)
{
    kill(0,SIGHUP); //将信号SIGHUP传给和目前进程相同进程组的所有进程
}

char hostSubnet[20] = {0};
char interf[6] = {0};

int main(int argc,char** argv)
{
    if (argc != 3)
    {
        printf( "param err  \n " );
        return -1;
    }
    snprintf(hostSubnet, 20, "%s", argv[1]);
    snprintf(interf, 6, "%s", argv[2]);

    printf("\n ++++ %s-%s-%d:   %s++++\n", __FILE__, __FUNCTION__, __LINE__ , interf);

    int i,x[255];
    pthread_t tid[255];
    int64_t timestamp = 0;
    for(i=0; i<255; i++)
        x[i]=i;
    setuid(getuid()); //放弃特权
    signal(SIGALRM,Alarm); //当信号SIGALRM到达时跳转到Alarm函数执行
    timestamp=TimerGetTickCount();
    for(i=1; i<255; i++)
        if((errno=pthread_create(&tid[i],NULL,recv_packet,&x[i])) <0)
            perror("pthread:");
    alarm(6); //用alarm函数设置timer超时
    for(i=1; i<255; i++)
        pthread_join(tid[i],NULL); //等待线程结束
    printf("total time: %d ms.\n",TimerGetTickCount()-timestamp);
}

void* recv_packet(void* arg)
{
    struct sockaddr_in dest;
    int fd;
    struct tcphdr* tcp;
    u_short sourcePort;
    struct servent* sptr;
    int startip,hostsums,port;

    struct sockaddr_in* in1;
    char* srcaddr;
    int loopend;
    int size;
    u_char readbuff[1600];
    struct sockaddr from;
    int from_len;
    int64_t Timestamp = 0;

    struct hostent* phe;
    struct ifreq if_data;
    u_long addr_p;
    char* addr;
    char remoteIP[15];
    char shellCmd[32];

    //发送
    port=atoi("111");		//remote port
    sourcePort=20000+*(int*)arg;	//local port
    sprintf(remoteIP,"%s%d",hostSubnet,*(int*)arg);		//remote ip
    if ((fd=socket(AF_INET,SOCK_RAW,IPPROTO_TCP)) <0) //在这个fd上发数据,系统会自动给你加个IP头的,所以你只要自己构造TCP头就是了
        perror("socket");
    strcpy (if_data.ifr_name, interf );
    if (ioctl (fd, SIOCGIFADDR, &if_data) < 0) //取名为eth0的的IP地址,这是个interface的地址
    {
        perror("ioctl");
        return NULL;
    }
    memcpy ((void*) &addr_p, (void*) &if_data.ifr_addr.sa_data + 2, 4); //把它放到addr_p中
    bzero(&dest,sizeof(dest));
    dest.sin_family=AF_INET;
    if (phe=gethostbyname(remoteIP))
        memcpy(&dest.sin_addr,phe-> h_addr,phe-> h_length);
    else if (inet_aton(remoteIP,&dest.sin_addr) <0)
        perror("host");
    dest.sin_port=htons(port);
    if (sendSyn(fd,addr_p,sourcePort,SEQ,&dest) <0)
        perror("send");
    dest.sin_addr.s_addr=htonl(ntohl(dest.sin_addr.s_addr)+1);

    //接收
    tcp=(struct tcphdr *)(readbuff+20); //那个fd中读出的数据包括了IP头的所以+20
    Timestamp=TimerGetTickCount();
    for (;;)
    {
        if(TimerGetTickCount()-Timestamp>3000)
        {
            //printf("thread[%d] (timeout)dead!\n",*(int*)arg);
            break;
        }
        from_len = sizeof(struct sockaddr);
        size = recvfrom(fd, (char*)readbuff, 1600, MSG_DONTWAIT, &from, &from_len);
        if ( size <(20+20) ) /*读出的数据小于两个头的最小长度的话continue*/
        {
            usleep(100);
            continue;
        }
        if ( (ntohl(tcp-> ack_seq)!=SEQ+1)|| (ntohs(tcp-> dest)!=sourcePort))
        {
            //printf("thread[%d] ignore!\n",*(int*)arg);
            usleep(100);
            continue;
        }
        /* RST/ACK - no service listening on port*/
        if (tcp-> rst && tcp-> ack)
        {
           // printf("thread[%d] dead!\n",*(int*)arg);
            break;
        }
        /* SYN/ACK -Service listening on this port*/
        if (tcp-> ack && tcp-> syn)
        {
            in1=(struct sockaddr_in*)&from;
            srcaddr=inet_ntoa(in1->sin_addr);
            printf("==========thread[%d] %s alive!\n",*(int*)arg,srcaddr);
            sprintf(shellCmd,"showmount %s",srcaddr);
            system(shellCmd);
            printf("%s\n",shellCmd);
            fflush(stdout);
            break;
        }
    }/* end for*/
        return NULL;
}

int sendSyn(int sendSocket,u_long sourceIP,u_short sourcePort,u_long seq,struct sockaddr_in* dst)
{
    unsigned char netPacket[TCPSIZE];
    struct   tcphdr* tcp;
    u_char* pPseudoHead;
    u_char pseudoHead[12+sizeof(struct tcphdr)];
    u_short   tcpHeadLen;
    memset(netPacket,0,TCPSIZE);
    tcpHeadLen=htons(sizeof(struct tcphdr));
    tcp=(struct tcphdr*)netPacket;
    tcp-> source=htons(sourcePort);
    tcp-> dest=dst-> sin_port;
    tcp-> seq=htonl(seq);
    tcp-> ack_seq=0;
    tcp-> doff=5;
    tcp-> syn=1;
    tcp-> window=htons(10052);
    tcp-> check=0;
    tcp-> urg_ptr=0;
    pPseudoHead=pseudoHead;
    memset(pPseudoHead,0,12+sizeof(struct tcphdr));
    memcpy(pPseudoHead,&sourceIP,4);
    pPseudoHead+=4;
    memcpy(pPseudoHead,&dst->sin_addr,4);
    pPseudoHead+=5;
    memset(pPseudoHead,6,1);
    pPseudoHead++;
    memcpy(pPseudoHead,&tcpHeadLen,2);
    pPseudoHead+=2;
    memcpy(pPseudoHead,tcp,sizeof(struct tcphdr));
    tcp-> check=in_cksum((u_short*)pseudoHead,sizeof(struct tcphdr)+12);
    return (sendto(sendSocket,netPacket,TCPSIZE,0,(struct sockaddr*)dst,sizeof(struct sockaddr_in)));
}
