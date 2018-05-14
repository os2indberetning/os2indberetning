using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Core.DmzModel;
using System.Security.Cryptography;
using Core.DomainModel;


namespace Core.DomainServices.Encryption
{
    public static class Encryptor
    {
        public const string EncryptKey = "testpasswordkey";

        public static Profile EncryptProfile(Profile profile)
        {
            try
            {
                profile.FirstName = StringCipher.Encrypt(profile.FirstName, EncryptKey);
                profile.LastName = StringCipher.Encrypt(profile.LastName, EncryptKey);
                profile.HomeLatitude = StringCipher.Encrypt(profile.HomeLatitude, EncryptKey);
                profile.HomeLongitude = StringCipher.Encrypt(profile.HomeLongitude, EncryptKey);
                profile.FullName = StringCipher.Encrypt(profile.FullName, EncryptKey);
                return profile;
            }
            catch (Exception ex)
            {
                //This should be logged when ILogger is enabled
                Console.WriteLine($"Class Encryptor, Encryptprofile(). Exception= {ex.Message}, in profile HomeLatitude: {profile.HomeLatitude}, HomeLongitude: {profile.HomeLongitude}, ID: {profile.Id}");
                return profile;
            }
        }

        public static Profile DecryptProfile(Profile profile)
        {
            try
            {
                profile.FirstName = StringCipher.Decrypt(profile.FirstName, EncryptKey);
                profile.LastName = StringCipher.Decrypt(profile.LastName, EncryptKey);
                profile.HomeLatitude = StringCipher.Decrypt(profile.HomeLatitude, EncryptKey);
                profile.HomeLongitude = StringCipher.Decrypt(profile.HomeLongitude, EncryptKey);
                profile.FullName = StringCipher.Decrypt(profile.FullName, EncryptKey);
                return profile;
            }
            catch (FormatException fe)
            {
                //This should be logged when ILogger is enabled
                Console.WriteLine($"Class Encryptor, FormatException DecryptProfile() i Encryptor= {fe.Message}, HomeLatitude: {profile.HomeLatitude}, HomeLongitude: {profile.HomeLongitude}, ID: {profile.Id}");
                return profile;
            }
            catch (CryptographicException ce)
            {
                //This should be logged when ILogger is enabled
                Console.WriteLine($"Class Encryptor, CryptographicException DecryptProfile() i Encryptor= {ce.Message}, HomeLatitude: {profile.HomeLatitude}, HomeLongitude: {profile.HomeLongitude}, ID: {profile.Id}");
                return profile;
            }
        }

        public static DmzModel.Employment EncryptEmployment(DmzModel.Employment employment)
        {
            employment.EmploymentPosition = StringCipher.Encrypt(employment.EmploymentPosition, EncryptKey);
            return employment;
        }

        public static GPSCoordinate DecryptGPSCoordinate(GPSCoordinate gpscoord)
        {
            gpscoord.Latitude = StringCipher.Decrypt(gpscoord.Latitude, EncryptKey);
            gpscoord.Longitude = StringCipher.Decrypt(gpscoord.Longitude, EncryptKey);
            return gpscoord;
        }

        public static Token EncryptToken(Token token)
        {
            token.GuId = StringCipher.Encrypt(token.GuId, EncryptKey);
            token.TokenString = StringCipher.Encrypt(token.TokenString, EncryptKey);
            return token;
        }

        public static Token DecryptToken(Token token)
        {
            token.GuId = StringCipher.Decrypt(token.GuId, EncryptKey);
            token.TokenString = StringCipher.Decrypt(token.TokenString, EncryptKey);
            return token;
        }

        public static AppLogin EncryptAppLogin(AppLogin appLogin)
        {
            appLogin.GuId = StringCipher.Encrypt(appLogin.GuId, EncryptKey);
            appLogin.UserName = StringCipher.Encrypt(appLogin.Person.Initials, EncryptKey);
            return appLogin;
        }

        public static AppLogin DecryptAppLogin(AppLogin appLogin)
        {
            appLogin.GuId = StringCipher.Decrypt(appLogin.GuId, EncryptKey);
            appLogin.UserName = StringCipher.Decrypt(appLogin.UserName, EncryptKey);
            return appLogin;
        }

        public static string DecryptUser(string encryptedUser)
        {
            return StringCipher.Decrypt(encryptedUser, EncryptKey);
        }

    }
}
