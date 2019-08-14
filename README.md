# sesam-cifs
Simple service to list content  of CIFS shares as JSON (or download files)

Available endpoints: 
* /list/{share}/{path to dir} - to list share content
* /get/{share}/{path to file} - to download file from given share

### System Setup

```json
{
  "_id": "<system name>",
  "type": "system:microservice",
  "docker": {
    "environment": {
      "CIFS_DOMAIN": "",
      "CIFS_HOSTNAME": "<host name>",
      "CIFS_PASSWORD": "$SECRET(password)",
      "CIFS_USERNAME": "JonSnow"
    },
    "image": "<image name:tag>",
    "port": 8080
  },
  "verify_ssl": true
}
```


### Pipe Setup
```json
{
  "_id": "<name>",
  "type": "pipe",
  "source": {
    "type": "json",
    "system": "<system name>",
    "url": "/list/ShareName/Path/To/Folder"
  },
  "transform": {
    "type": "dtl",
    "rules": {
      "default": [
        ["add", "_id", "_S.name"]
      ]
    }
  }
}
```
