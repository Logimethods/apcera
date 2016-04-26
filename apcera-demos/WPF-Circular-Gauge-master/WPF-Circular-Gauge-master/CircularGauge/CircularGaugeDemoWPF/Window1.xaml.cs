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
      

        Game game1;
        Game game2;
        Game game3;
        Game game4;

        public Window1()
        {
            InitializeComponent();
            this.Loaded += new RoutedEventHandler(Window1_Loaded);
            r = new Random();
        }

        void Window1_Loaded(object sender, RoutedEventArgs e)
        {
            //Set the current value of the gauges
            game1 = new Game(1);
            this.myGauge1.DataContext = game1;
            game2 = new Game(2);
            this.myGauge2.DataContext = game2;
            game3 = new Game(3);
            this.myGauge3.DataContext = game3;
            game4 = new Game(4);
            this.myGauge4.DataContext = game4;

            //Start the timer
            timer1 = new DispatcherTimer();
            timer1.Interval = TimeSpan.FromMilliseconds(2000);
            timer1.Tick += new EventHandler(timer_Tick1);
            timer1.Start();

            timer2 = new DispatcherTimer();
            timer2.Interval = TimeSpan.FromMilliseconds(1500);
            timer2.Tick += new EventHandler(timer_Tick2);
            timer2.Start();
        }

        void timer_Tick1(object sender, EventArgs e)
        {
            //Update random scores
            //Random r = new Random();
            game1.Score = r.Next(99, 112);
            game2.Score = r.Next(99, 112);
            //game3.Score = r.Next(99, 112);
            //game4.Score = r.Next(99, 112);

        }

        void timer_Tick2(object sender, EventArgs e)
        {
            //Update random scores
            //Random r = new Random();
           
            game3.Score = r.Next(99, 112);
            game4.Score = r.Next(99, 112);

        }

    }

    /// <summary>
    /// Helper class to simulate a game
    /// </summary>
    public class Game : INotifyPropertyChanged
    {
        private double score;
        private int meterId;

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
                  

            // Send data to camel       
            String message = "VOLTAGE=" + score.ToString() + " " + "ID=" + meterId.ToString() + "\n";
            byte[] toSendBytes = System.Text.Encoding.ASCII.GetBytes(message);
            clientSocket.Send(toSendBytes);
            //clientSocket.Close();
        }


        public Game(int meterId)
        {
            this.Score = 0;
            this.meterId = meterId;
            this.ipAddress = IPAddress.Parse("127.0.0.1");
            this.remoteEP = new IPEndPoint(ipAddress, 9999);
            this.clientSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
            clientSocket.Connect(remoteEP);  
        }


        #region INotifyPropertyChanged Members

        public event PropertyChangedEventHandler PropertyChanged;

        #endregion
    }
}
