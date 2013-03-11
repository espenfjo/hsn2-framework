#
# This Makefile is responsoble for build debian package.
# 4 variables must be provided:
HSN2_VER=2.0.0-SNAPSHOT
BUILD_NUMBER=1
DEBIAN_DIST=sid
HSN2_COMPONENT=framework

all:	hsn2-framework

clean:	hsn2-framework-clean

PKG=hsn2-$(HSN2_COMPONENT)_$(HSN2_VER)-$(BUILD_NUMBER)_all
hsn2-framework: hsn2-framework-clean
	mkdir -p $(PKG)/DEBIAN
	mkdir -p $(PKG)/etc/hsn2/workflows
	mkdir -p $(PKG)/etc/init.d
	mkdir -p $(PKG)/opt/hsn2/$(HSN2_COMPONENT)
	mkdir -p $(PKG)/var/log/hsn2
	tar -zxf ../target/hsn2-framework-*.tar.gz -C $(PKG)/opt/hsn2/
	mv  $(PKG)/opt/hsn2/$(HSN2_COMPONENT)/hsn2-main-$(HSN2_VER).jar $(PKG)/opt/hsn2/$(HSN2_COMPONENT)/hsn2-$(HSN2_COMPONENT).jar
	cp hsn2-fwk.initd $(PKG)/etc/init.d/hsn2-$(HSN2_COMPONENT)
	cp hsn2-fwk.postinst $(PKG)/DEBIAN/postinst
#	cp hsn2-fwk.sh $(PKG)/opt/hsn2/$(HSN2_COMPONENT)/hsn2-$(HSN2_COMPONENT).sh
#hack to solve SVN problem with handling symlinks
#	eval `cat config.cfg` $(PKG)/etc/hsn2/$(HSN2_COMPONENT).conf
	cp ../hsn2-configuration/src/main/resources/defaultConfig.cfg $(PKG)/etc/hsn2/$(HSN2_COMPONENT).conf
#original line
#	cp config.cfg $(PKG)/etc/hsn2/$(HSN2_COMPONENT).cfg
	cp control conffiles ${PKG}/DEBIAN
	sed -i "s/{VER}/${HSN2_VER}-${BUILD_NUMBER}/" $(PKG)/DEBIAN/control
	sed -i "s/{DEBIAN_DIST}/${DEBIAN_DIST}/" $(PKG)/DEBIAN/control
	fakeroot dpkg -b $(PKG)

hsn2-framework-clean:
	rm -rf hsn2-$(HSN2_COMPONENT)_*
