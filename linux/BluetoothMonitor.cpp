#include "BluetoothMonitor.h"
#include "logger.h"

#include <QDebug>
#include <QDBusObjectPath>
#include <QDBusMetaType>
#include <QProcess>
#include <QDir>
#include <QDirIterator>
#include <QRegularExpression>
#include <QFile>

BluetoothMonitor::BluetoothMonitor(QObject *parent)
    : QObject(parent), m_dbus(QDBusConnection::systemBus())
{
    // Register meta-types for D-Bus interaction
    qDBusRegisterMetaType<QDBusObjectPath>();
    qDBusRegisterMetaType<ManagedObjectList>();

    if (!m_dbus.isConnected())
    {
        LOG_WARN("Failed to connect to system D-Bus");
        return;
    }

    registerDBusService();
    checkAlreadyConnectedDevices(); // Check for already connected devices on startup
}

BluetoothMonitor::~BluetoothMonitor()
{
    m_dbus.disconnectFromBus(m_dbus.name());
}

void BluetoothMonitor::registerDBusService()
{
    // Match signals for PropertiesChanged on any BlueZ Device interface
    if (!m_dbus.connect("", "", "org.freedesktop.DBus.Properties", "PropertiesChanged",
                        this, SLOT(onPropertiesChanged(QString, QVariantMap, QStringList))))
    {
        LOG_WARN("Failed to connect to D-Bus PropertiesChanged signal");
    }
}

bool BluetoothMonitor::isAirPodsDevice(const QString &devicePath)
{
    QDBusInterface deviceInterface("org.bluez", devicePath, "org.freedesktop.DBus.Properties", m_dbus);

    // Get UUIDs to check if it's an AirPods device
    QDBusReply<QVariant> uuidsReply = deviceInterface.call("Get", "org.bluez.Device1", "UUIDs");
    if (!uuidsReply.isValid())
    {
        return false;
    }

    QStringList uuids = uuidsReply.value().toStringList();
    return uuids.contains("74ec2172-0bad-4d01-8f77-997b2be0722a");
}

QString BluetoothMonitor::getDevicePath(const QString &macAddress)
{
    // Convert MAC address to lowercase and replace colons with underscores
    QString formattedMac = macAddress.toLower().replace(":", "_");
    
    // List all BlueZ devices
    QDBusInterface objectManager("org.bluez", "/", "org.freedesktop.DBus.ObjectManager", m_dbus);
    QDBusMessage reply = objectManager.call("GetManagedObjects");
    
    if (reply.type() == QDBusMessage::ReplyMessage) {
        ManagedObjectList objects = qdbus_cast<ManagedObjectList>(reply.arguments().at(0));
        
        // Look for the device with matching address
        for (const auto &path : objects.keys()) {
            QString objPath = path.path();
            if (objPath.contains(formattedMac)) {
                return objPath;
            }
        }
    }
    
    // If no matching device is found, try the default path
    return QString("/org/bluez/hci0/dev_%1").arg(formattedMac);
}

QStringList BluetoothMonitor::findAdapters()
{
    QStringList adapters;
    QDBusInterface manager("org.bluez", "/", "org.freedesktop.DBus.ObjectManager", m_dbus);
    QDBusMessage reply = manager.call("GetManagedObjects");
    
    if (reply.type() == QDBusMessage::ReplyMessage) {
        ManagedObjectList objects = qdbus_cast<ManagedObjectList>(reply.arguments().at(0));
        for (const auto &path : objects.keys()) {
            QString objPath = path.path();
            if (objPath.contains("/org/bluez/hci")) {
                adapters.append(objPath);
            }
        }
    }
    return adapters;
}

QString BluetoothMonitor::getDeviceNameFromBluetooth(const QString &macAddress)
{
    // Try all available adapters
    QStringList adapters = findAdapters();
    for (const QString &adapter : adapters) {
        QDBusInterface adapterInterface("org.bluez", adapter, "org.bluez.Adapter1", m_dbus);
        QDBusReply<QDBusObjectPath> deviceReply = adapterInterface.call("GetDevice", macAddress);
        
        if (deviceReply.isValid()) {
            QDBusInterface deviceInterface("org.bluez", deviceReply.value().path(), 
                                        "org.freedesktop.DBus.Properties", m_dbus);
            QDBusReply<QVariant> nameReply = deviceInterface.call("Get", "org.bluez.Device1", "Name");
            
            if (nameReply.isValid() && !nameReply.value().toString().isEmpty()) {
                return nameReply.value().toString();
            }
        }
    }
    return QString();
}

QString BluetoothMonitor::getDeviceNameFromBluetoothctl(const QString &macAddress)
{
    QProcess process;
    process.start("bluetoothctl", QStringList() << "info" << macAddress);
    process.waitForFinished(2000);
    
    if (process.exitCode() == 0) {
        QString output = QString::fromUtf8(process.readAllStandardOutput());
        QRegularExpression nameRegex("Name:\\s*(.+)\\n");
        QRegularExpressionMatch match = nameRegex.match(output);
        
        if (match.hasMatch()) {
            return match.captured(1).trimmed();
        }
    }
    return QString();
}

QString BluetoothMonitor::getDeviceNameFromCache(const QString &macAddress)
{
    // Check common cache locations
    QStringList cachePaths = {
        "/var/lib/bluetooth",
        QDir::homePath() + "/.cache/bluetooth"
    };
    
    QString formattedMac = macAddress.toLower();
    for (const QString &basePath : cachePaths) {
        QDirIterator it(basePath, QDir::Dirs | QDir::NoDotAndDotDot, QDirIterator::Subdirectories);
        while (it.hasNext()) {
            QString path = it.next();
            if (path.contains(formattedMac)) {
                QFile infoFile(path + "/info");
                if (infoFile.open(QIODevice::ReadOnly | QIODevice::Text)) {
                    QString content = QString::fromUtf8(infoFile.readAll());
                    QRegularExpression nameRegex("Name=(.+)\\n");
                    QRegularExpressionMatch match = nameRegex.match(content);
                    if (match.hasMatch()) {
                        return match.captured(1).trimmed();
                    }
                }
            }
        }
    }
    return QString();
}

QString BluetoothMonitor::getDeviceName(const QString &macAddress)
{
    LOG_INFO("Attempting to resolve name for device: " << macAddress);
    
    // First try the standard BlueZ D-Bus interface
    QString devicePath = getDevicePath(macAddress);
    LOG_INFO("Trying BlueZ D-Bus interface with path: " << devicePath);
    QDBusInterface deviceInterface("org.bluez", devicePath, "org.freedesktop.DBus.Properties", m_dbus);
    QDBusReply<QVariant> nameReply = deviceInterface.call("Get", "org.bluez.Device1", "Name");
    
    if (nameReply.isValid() && !nameReply.value().toString().isEmpty())
    {
        LOG_INFO("Found name via BlueZ D-Bus: " << nameReply.value().toString());
        return nameReply.value().toString();
    }

    // Try alternative BlueZ method
    LOG_INFO("Trying alternative BlueZ method...");
    QString name = getDeviceNameFromBluetooth(macAddress);
    if (!name.isEmpty()) {
        LOG_INFO("Found name via alternative BlueZ method: " << name);
        return name;
    }

    // Try bluetoothctl command
    LOG_INFO("Trying bluetoothctl command...");
    name = getDeviceNameFromBluetoothctl(macAddress);
    if (!name.isEmpty()) {
        LOG_INFO("Found name via bluetoothctl: " << name);
        return name;
    }

    // Try reading from cache
    LOG_INFO("Trying to read from cache...");
    name = getDeviceNameFromCache(macAddress);
    if (!name.isEmpty()) {
        LOG_INFO("Found name in cache: " << name);
        return name;
    }

    // If all methods fail, return the MAC address
    LOG_WARN("Could not resolve device name for MAC: " << macAddress);
    return macAddress;
}

bool BluetoothMonitor::checkAlreadyConnectedDevices()
{
    QDBusInterface objectManager("org.bluez", "/", "org.freedesktop.DBus.ObjectManager", m_dbus);
    QDBusMessage reply = objectManager.call("GetManagedObjects");

    if (reply.type() == QDBusMessage::ErrorMessage)
    {
        LOG_WARN("Failed to get managed objects: " << reply.errorMessage());
        return false;
    }

    QVariant firstArg = reply.arguments().constFirst();
    QDBusArgument arg = firstArg.value<QDBusArgument>();
    ManagedObjectList managedObjects;
    arg >> managedObjects;

    bool deviceFound = false;

    for (auto it = managedObjects.constBegin(); it != managedObjects.constEnd(); ++it)
    {
        const QDBusObjectPath &objPath = it.key();
        const QMap<QString, QVariantMap> &interfaces = it.value();

        if (interfaces.contains("org.bluez.Device1"))
        {
            const QVariantMap &deviceProps = interfaces.value("org.bluez.Device1");

            // Check if the device has the necessary properties
            if (!deviceProps.contains("UUIDs") || !deviceProps.contains("Connected") ||
                !deviceProps.contains("Address") || !deviceProps.contains("Name"))
            {
                continue;
            }

            QStringList uuids = deviceProps["UUIDs"].toStringList();
            bool isAirPods = uuids.contains("74ec2172-0bad-4d01-8f77-997b2be0722a");

            if (isAirPods)
            {
                bool connected = deviceProps["Connected"].toBool();
                if (connected)
                {
                    QString macAddress = deviceProps["Address"].toString();
                    QString deviceName = deviceProps["Name"].toString();
                    emit deviceConnected(macAddress, deviceName);
                    LOG_DEBUG("Found already connected AirPods: " << macAddress << " Name: " << deviceName);
                    deviceFound = true;
                }
            }
        }
    }
    return deviceFound;
}

void BluetoothMonitor::onPropertiesChanged(const QString &interface, const QVariantMap &changedProps, const QStringList &invalidatedProps)
{
    Q_UNUSED(invalidatedProps);

    if (interface != "org.bluez.Device1")
    {
        return;
    }

    if (changedProps.contains("Connected"))
    {
        bool connected = changedProps["Connected"].toBool();
        QString path = QDBusContext::message().path();

        if (!isAirPodsDevice(path))
        {
            return;
        }

        QDBusInterface deviceInterface("org.bluez", path, "org.freedesktop.DBus.Properties", m_dbus);

        // Get the device address
        QDBusReply<QVariant> addrReply = deviceInterface.call("Get", "org.bluez.Device1", "Address");
        if (!addrReply.isValid())
        {
            return;
        }
        QString macAddress = addrReply.value().toString();
        QString deviceName = getDeviceName(path);

        if (connected)
        {
            emit deviceConnected(macAddress, deviceName);
            LOG_DEBUG("AirPods device connected:" << macAddress << " Name:" << deviceName);
        }
        else
        {
            emit deviceDisconnected(macAddress, deviceName);
            LOG_DEBUG("AirPods device disconnected:" << macAddress << " Name:" << deviceName);
        }
    }
}