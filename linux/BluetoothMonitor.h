#ifndef BLUETOOTHMONITOR_H
#define BLUETOOTHMONITOR_H

#include <QObject>
#include <QtDBus/QtDBus>

// Forward declarations for D-Bus types
typedef QMap<QDBusObjectPath, QMap<QString, QVariantMap>> ManagedObjectList;
Q_DECLARE_METATYPE(ManagedObjectList)

class BluetoothMonitor : public QObject, protected QDBusContext
{
    Q_OBJECT
public:
    explicit BluetoothMonitor(QObject *parent = nullptr);
    ~BluetoothMonitor();

    bool checkAlreadyConnectedDevices();

signals:
    void deviceConnected(const QString &macAddress, const QString &deviceName);
    void deviceDisconnected(const QString &macAddress, const QString &deviceName);

private slots:
    void onPropertiesChanged(const QString &interface, const QVariantMap &changedProps, const QStringList &invalidatedProps);

private:
    QDBusConnection m_dbus;
    void registerDBusService();
    bool isAirPodsDevice(const QString &devicePath);
    QString getDevicePath(const QString &macAddress);
    QString getDeviceNameFromBluetooth(const QString &macAddress);
    QString getDeviceNameFromBluetoothctl(const QString &macAddress);
    QString getDeviceNameFromCache(const QString &macAddress);
    QStringList findAdapters();

public:
    QString getDeviceName(const QString &macAddress);
};

#endif // BLUETOOTHMONITOR_H