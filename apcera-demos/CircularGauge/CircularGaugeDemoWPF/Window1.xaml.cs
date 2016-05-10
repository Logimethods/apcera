using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;
using System.Windows.Threading;
using System.ComponentModel;
using System.Net.Sockets;
using System.Net;


namespace CircularGaugeDemoWPF
{
    /// <summary>
    /// Interaction logic for Window1.xaml
    /// </summary>
    public partial class Window1 : Window
    {
        //Private variables
        DispatcherTimer timer1;
        DispatcherTimer timer2;
        Random r;
      
        Meter meter1;
        Meter meter2;
        Meter meter3;
        Meter meter4;

        public Window1()
        {
            InitializeComponent();
            this.Loaded += new RoutedEventHandler(Window1_Loaded);
            r = new Random();
        }

        void Window1_Loaded(object sender, RoutedEventArgs e)
        {
            //Set the current value of the gauges
            meter1 = new Meter(1);
            this.myGauge1.DataContext = meter1;
            meter2 = new Meter(2);
            this.myGauge2.DataContext = meter2;
            meter3 = new Meter(3);
            this.myGauge3.DataContext = meter3;
            meter4 = new Meter(4);
            this.myGauge4.DataContext = meter4;

            //Start the timer
            timer1 = new DispatcherTimer();
            timer1.Interval = TimeSpan.FromMilliseconds(1000);
            timer1.Tick += new EventHandler(timer_Tick1);
            timer1.Start();

            timer2 = new DispatcherTimer();
            timer2.Interval = TimeSpan.FromMilliseconds(1500);
            timer2.Tick += new EventHandler(timer_Tick2);
            timer2.Start();
        }

        void timer_Tick1(object sender, EventArgs e)
        {
            //Update random voltage

            if (meter1.shorted)
            {
                meter1.Score = 120;
            }
            else
            {
                meter1.Score = r.Next(99, 112);
            }

            if (meter2.shorted)
            {
                meter2.Score = 120;
            }
            else
            {
                meter2.Score = r.Next(99, 113);
            }

        }

        void timer_Tick2(object sender, EventArgs e)
        {
            //Update random voltage          
            meter3.Score = r.Next(99, 113);
            meter4.Score = r.Next(99, 112);

        }

        private void CheckBox1_Checked(object sender, RoutedEventArgs e)
        {
            meter1.shorted =  ((CheckBox)sender).IsChecked.Value;
        }
        private void CheckBox1_Unchecked(object sender, RoutedEventArgs e)
        {
            meter1.shorted = ((CheckBox)sender).IsChecked.Value;
        }

        private void CheckBox2_Checked(object sender, RoutedEventArgs e)
        {
            meter2.shorted = ((CheckBox)sender).IsChecked.Value;
        }
        private void CheckBox2_Unchecked(object sender, RoutedEventArgs e)
        {
            meter2.shorted = ((CheckBox)sender).IsChecked.Value;
        }

        private void Connect_Button_Click(object sender, RoutedEventArgs e)
        {
            meter1.ConnectToCamel();
            meter2.ConnectToCamel();
            meter3.ConnectToCamel();
            meter4.ConnectToCamel();
        }

    }

    /// <summary>
    /// Helper class to simulate a game
    /// </summary>
    public class Meter : INotifyPropertyChanged
    {
        private double score;
        private int meterId;
        public bool shorted = false;
        public bool connectToCamel = false;

        IPAddress ipAddress;
        IPEndPoint remoteEP;
        Socket clientSocket;

        public double Score
        {
            get { return score; }
            set
            {
                score = value;
                if (PropertyChanged != null)
                {
                    PropertyChanged(this, new PropertyChangedEventArgs("Score"));
                    NotifyCamel();
                }
            }
        }

        private void NotifyCamel()
        {
            // Connect to the remote endpoint.

            if (this.connectToCamel)
            {
                // Send data to camel       
                String message = "VOLTAGE=" + score.ToString() + " " + "ID=" + meterId.ToString() + "\n";
                byte[] toSendBytes = System.Text.Encoding.ASCII.GetBytes(message);
                clientSocket.Send(toSendBytes);
                //clientSocket.Close();
            }
        }

        public void ConnectToCamel()
        {
            this.connectToCamel = true;
            this.ipAddress = IPAddress.Parse("127.0.0.1");  //192.168.0.107:38734 192.168.0.107:47023
            this.remoteEP = new IPEndPoint(ipAddress, 9999);
            //this.ipAddress = IPAddress.Parse("192.168.64.129");  //192.168.0.107:38734 192.168.0.107:47023
            //this.remoteEP = new IPEndPoint(ipAddress, 54398);
            this.clientSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
            clientSocket.Connect(remoteEP);  
        }

        public Meter(int meterId)
        {
            this.Score = 0;
            this.meterId = meterId;
            connectToCamel = false;           
        }


        #region INotifyPropertyChanged Members

        public event PropertyChangedEventHandler PropertyChanged;

        #endregion
    }
}
