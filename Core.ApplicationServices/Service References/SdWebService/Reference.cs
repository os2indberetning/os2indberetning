﻿//------------------------------------------------------------------------------
// <auto-generated>
//     This code was generated by a tool.
//     Runtime Version:4.0.30319.42000
//
//     Changes to this file may cause incorrect behavior and will be lost if
//     the code is regenerated.
// </auto-generated>
//------------------------------------------------------------------------------

namespace Core.ApplicationServices.SdWebService {
    
    
    [System.CodeDom.Compiler.GeneratedCodeAttribute("System.ServiceModel", "4.0.0.0")]
    [System.ServiceModel.ServiceContractAttribute(Namespace="www.sd.dk/sdws/KoerselOpret20120201", ConfigurationName="SdWebService.KoerselOpret20120201PortType")]
    public interface KoerselOpret20120201PortType {
        
        // CODEGEN: Generating message contract since the operation KoerselOpret20120201Operation is neither RPC nor document wrapped.
        [System.ServiceModel.OperationContractAttribute(Action="https://service.sd.dk/sdws/services/KoerselOpret20120201", ReplyAction="*")]
        [System.ServiceModel.XmlSerializerFormatAttribute(SupportFaults=true)]
        Core.ApplicationServices.SdWebService.KoerselOpret20120201OperationResponse KoerselOpret20120201Operation(Core.ApplicationServices.SdWebService.KoerselOpret20120201OperationRequest request);
        
        [System.ServiceModel.OperationContractAttribute(Action="https://service.sd.dk/sdws/services/KoerselOpret20120201", ReplyAction="*")]
        System.Threading.Tasks.Task<Core.ApplicationServices.SdWebService.KoerselOpret20120201OperationResponse> KoerselOpret20120201OperationAsync(Core.ApplicationServices.SdWebService.KoerselOpret20120201OperationRequest request);
    }
    
    /// <remarks/>
    [System.CodeDom.Compiler.GeneratedCodeAttribute("System.Xml", "4.7.2102.0")]
    [System.SerializableAttribute()]
    [System.Diagnostics.DebuggerStepThroughAttribute()]
    [System.ComponentModel.DesignerCategoryAttribute("code")]
    [System.Xml.Serialization.XmlTypeAttribute(Namespace="urn:oio:sd:snitflader:2012.02.01")]
    public partial class KoerselOpretRequestType : object, System.ComponentModel.INotifyPropertyChanged {
        
        private string institutionIdentifikatorField;
        
        private string personnummerIdentifikatorField;
        
        private string ansaettelseIdentifikatorField;
        
        private string registreringTypeIdentifikatorField;
        
        private System.DateTime koerselDatoField;
        
        private decimal kilometerMaalField;
        
        private bool regel60DageIndikatorField;
        
        private string koertFraTekstField;
        
        private string koertTilTekstField;
        
        private string aarsagTekstField;
        
        /// <remarks/>
        [System.Xml.Serialization.XmlElementAttribute(Order=0)]
        public string InstitutionIdentifikator {
            get {
                return this.institutionIdentifikatorField;
            }
            set {
                this.institutionIdentifikatorField = value;
                this.RaisePropertyChanged("InstitutionIdentifikator");
            }
        }
        
        /// <remarks/>
        [System.Xml.Serialization.XmlElementAttribute(Order=1)]
        public string PersonnummerIdentifikator {
            get {
                return this.personnummerIdentifikatorField;
            }
            set {
                this.personnummerIdentifikatorField = value;
                this.RaisePropertyChanged("PersonnummerIdentifikator");
            }
        }
        
        /// <remarks/>
        [System.Xml.Serialization.XmlElementAttribute(Order=2)]
        public string AnsaettelseIdentifikator {
            get {
                return this.ansaettelseIdentifikatorField;
            }
            set {
                this.ansaettelseIdentifikatorField = value;
                this.RaisePropertyChanged("AnsaettelseIdentifikator");
            }
        }
        
        /// <remarks/>
        [System.Xml.Serialization.XmlElementAttribute(Order=3)]
        public string RegistreringTypeIdentifikator {
            get {
                return this.registreringTypeIdentifikatorField;
            }
            set {
                this.registreringTypeIdentifikatorField = value;
                this.RaisePropertyChanged("RegistreringTypeIdentifikator");
            }
        }
        
        /// <remarks/>
        [System.Xml.Serialization.XmlElementAttribute(DataType="date", Order=4)]
        public System.DateTime KoerselDato {
            get {
                return this.koerselDatoField;
            }
            set {
                this.koerselDatoField = value;
                this.RaisePropertyChanged("KoerselDato");
            }
        }
        
        /// <remarks/>
        [System.Xml.Serialization.XmlElementAttribute(Order=5)]
        public decimal KilometerMaal {
            get {
                return this.kilometerMaalField;
            }
            set {
                this.kilometerMaalField = value;
                this.RaisePropertyChanged("KilometerMaal");
            }
        }
        
        /// <remarks/>
        [System.Xml.Serialization.XmlElementAttribute(Order=6)]
        public bool Regel60DageIndikator {
            get {
                return this.regel60DageIndikatorField;
            }
            set {
                this.regel60DageIndikatorField = value;
                this.RaisePropertyChanged("Regel60DageIndikator");
            }
        }
        
        /// <remarks/>
        [System.Xml.Serialization.XmlElementAttribute(Order=7)]
        public string KoertFraTekst {
            get {
                return this.koertFraTekstField;
            }
            set {
                this.koertFraTekstField = value;
                this.RaisePropertyChanged("KoertFraTekst");
            }
        }
        
        /// <remarks/>
        [System.Xml.Serialization.XmlElementAttribute(Order=8)]
        public string KoertTilTekst {
            get {
                return this.koertTilTekstField;
            }
            set {
                this.koertTilTekstField = value;
                this.RaisePropertyChanged("KoertTilTekst");
            }
        }
        
        /// <remarks/>
        [System.Xml.Serialization.XmlElementAttribute(Order=9)]
        public string AarsagTekst {
            get {
                return this.aarsagTekstField;
            }
            set {
                this.aarsagTekstField = value;
                this.RaisePropertyChanged("AarsagTekst");
            }
        }
        
        public event System.ComponentModel.PropertyChangedEventHandler PropertyChanged;
        
        protected void RaisePropertyChanged(string propertyName) {
            System.ComponentModel.PropertyChangedEventHandler propertyChanged = this.PropertyChanged;
            if ((propertyChanged != null)) {
                propertyChanged(this, new System.ComponentModel.PropertyChangedEventArgs(propertyName));
            }
        }
    }
    
    /// <remarks/>
    [System.CodeDom.Compiler.GeneratedCodeAttribute("System.Xml", "4.7.2102.0")]
    [System.SerializableAttribute()]
    [System.Diagnostics.DebuggerStepThroughAttribute()]
    [System.ComponentModel.DesignerCategoryAttribute("code")]
    [System.Xml.Serialization.XmlTypeAttribute(Namespace="urn:oio:sd:snitflader:2012.02.01")]
    public partial class KoerselOpret20120201Type : object, System.ComponentModel.INotifyPropertyChanged {
        
        private KoerselOpretRequestType inddataStrukturField;
        
        private System.DateTime dannetDatoTidField;
        
        /// <remarks/>
        [System.Xml.Serialization.XmlElementAttribute(Order=0)]
        public KoerselOpretRequestType InddataStruktur {
            get {
                return this.inddataStrukturField;
            }
            set {
                this.inddataStrukturField = value;
                this.RaisePropertyChanged("InddataStruktur");
            }
        }
        
        /// <remarks/>
        [System.Xml.Serialization.XmlAttributeAttribute()]
        public System.DateTime dannetDatoTid {
            get {
                return this.dannetDatoTidField;
            }
            set {
                this.dannetDatoTidField = value;
                this.RaisePropertyChanged("dannetDatoTid");
            }
        }
        
        public event System.ComponentModel.PropertyChangedEventHandler PropertyChanged;
        
        protected void RaisePropertyChanged(string propertyName) {
            System.ComponentModel.PropertyChangedEventHandler propertyChanged = this.PropertyChanged;
            if ((propertyChanged != null)) {
                propertyChanged(this, new System.ComponentModel.PropertyChangedEventArgs(propertyName));
            }
        }
    }
    
    [System.Diagnostics.DebuggerStepThroughAttribute()]
    [System.CodeDom.Compiler.GeneratedCodeAttribute("System.ServiceModel", "4.0.0.0")]
    [System.ComponentModel.EditorBrowsableAttribute(System.ComponentModel.EditorBrowsableState.Advanced)]
    [System.ServiceModel.MessageContractAttribute(IsWrapped=false)]
    public partial class KoerselOpret20120201OperationRequest {
        
        [System.ServiceModel.MessageBodyMemberAttribute(Namespace="urn:oio:sd:snitflader:2012.02.01", Order=0)]
        public Core.ApplicationServices.SdWebService.KoerselOpretRequestType InddataStruktur;
        
        public KoerselOpret20120201OperationRequest() {
        }
        
        public KoerselOpret20120201OperationRequest(Core.ApplicationServices.SdWebService.KoerselOpretRequestType InddataStruktur) {
            this.InddataStruktur = InddataStruktur;
        }
    }
    
    [System.Diagnostics.DebuggerStepThroughAttribute()]
    [System.CodeDom.Compiler.GeneratedCodeAttribute("System.ServiceModel", "4.0.0.0")]
    [System.ComponentModel.EditorBrowsableAttribute(System.ComponentModel.EditorBrowsableState.Advanced)]
    [System.ServiceModel.MessageContractAttribute(IsWrapped=false)]
    public partial class KoerselOpret20120201OperationResponse {
        
        [System.ServiceModel.MessageBodyMemberAttribute(Namespace="urn:oio:sd:snitflader:2012.02.01", Order=0)]
        public Core.ApplicationServices.SdWebService.KoerselOpret20120201Type KoerselOpret20120201;
        
        public KoerselOpret20120201OperationResponse() {
        }
        
        public KoerselOpret20120201OperationResponse(Core.ApplicationServices.SdWebService.KoerselOpret20120201Type KoerselOpret20120201) {
            this.KoerselOpret20120201 = KoerselOpret20120201;
        }
    }
    
    [System.CodeDom.Compiler.GeneratedCodeAttribute("System.ServiceModel", "4.0.0.0")]
    public interface KoerselOpret20120201PortTypeChannel : Core.ApplicationServices.SdWebService.KoerselOpret20120201PortType, System.ServiceModel.IClientChannel {
    }
    
    [System.Diagnostics.DebuggerStepThroughAttribute()]
    [System.CodeDom.Compiler.GeneratedCodeAttribute("System.ServiceModel", "4.0.0.0")]
    public partial class KoerselOpret20120201PortTypeClient : System.ServiceModel.ClientBase<Core.ApplicationServices.SdWebService.KoerselOpret20120201PortType>, Core.ApplicationServices.SdWebService.KoerselOpret20120201PortType {
        
        public KoerselOpret20120201PortTypeClient() {
        }
        
        public KoerselOpret20120201PortTypeClient(string endpointConfigurationName) : 
                base(endpointConfigurationName) {
        }
        
        public KoerselOpret20120201PortTypeClient(string endpointConfigurationName, string remoteAddress) : 
                base(endpointConfigurationName, remoteAddress) {
        }
        
        public KoerselOpret20120201PortTypeClient(string endpointConfigurationName, System.ServiceModel.EndpointAddress remoteAddress) : 
                base(endpointConfigurationName, remoteAddress) {
        }
        
        public KoerselOpret20120201PortTypeClient(System.ServiceModel.Channels.Binding binding, System.ServiceModel.EndpointAddress remoteAddress) : 
                base(binding, remoteAddress) {
        }
        
        [System.ComponentModel.EditorBrowsableAttribute(System.ComponentModel.EditorBrowsableState.Advanced)]
        Core.ApplicationServices.SdWebService.KoerselOpret20120201OperationResponse Core.ApplicationServices.SdWebService.KoerselOpret20120201PortType.KoerselOpret20120201Operation(Core.ApplicationServices.SdWebService.KoerselOpret20120201OperationRequest request) {
            return base.Channel.KoerselOpret20120201Operation(request);
        }
        
        public Core.ApplicationServices.SdWebService.KoerselOpret20120201Type KoerselOpret20120201Operation(Core.ApplicationServices.SdWebService.KoerselOpretRequestType InddataStruktur) {
            Core.ApplicationServices.SdWebService.KoerselOpret20120201OperationRequest inValue = new Core.ApplicationServices.SdWebService.KoerselOpret20120201OperationRequest();
            inValue.InddataStruktur = InddataStruktur;
            Core.ApplicationServices.SdWebService.KoerselOpret20120201OperationResponse retVal = ((Core.ApplicationServices.SdWebService.KoerselOpret20120201PortType)(this)).KoerselOpret20120201Operation(inValue);
            return retVal.KoerselOpret20120201;
        }
        
        [System.ComponentModel.EditorBrowsableAttribute(System.ComponentModel.EditorBrowsableState.Advanced)]
        System.Threading.Tasks.Task<Core.ApplicationServices.SdWebService.KoerselOpret20120201OperationResponse> Core.ApplicationServices.SdWebService.KoerselOpret20120201PortType.KoerselOpret20120201OperationAsync(Core.ApplicationServices.SdWebService.KoerselOpret20120201OperationRequest request) {
            return base.Channel.KoerselOpret20120201OperationAsync(request);
        }
        
        public System.Threading.Tasks.Task<Core.ApplicationServices.SdWebService.KoerselOpret20120201OperationResponse> KoerselOpret20120201OperationAsync(Core.ApplicationServices.SdWebService.KoerselOpretRequestType InddataStruktur) {
            Core.ApplicationServices.SdWebService.KoerselOpret20120201OperationRequest inValue = new Core.ApplicationServices.SdWebService.KoerselOpret20120201OperationRequest();
            inValue.InddataStruktur = InddataStruktur;
            return ((Core.ApplicationServices.SdWebService.KoerselOpret20120201PortType)(this)).KoerselOpret20120201OperationAsync(inValue);
        }
    }
}
